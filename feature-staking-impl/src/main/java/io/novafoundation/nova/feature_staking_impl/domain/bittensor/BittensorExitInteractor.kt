package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorConfigRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitRuntimeSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitPreflight
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSwapSource
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class BittensorExitInteractor(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val exitRuntimeSource: BittensorExitRuntimeSource,
    private val subnetMetadataInteractor: BittensorSubnetMetadataInteractor,
    private val swapSource: BittensorSwapSource,
    private val configRepository: BittensorConfigRepository,
    val slippage: BittensorSlippage,
    private val extrinsicService: ExtrinsicService,
    private val submissionExecutor: BittensorSubmissionExecutor,
    private val walletConstants: WalletConstants,
    private val beneficiaryAddress: String,
    private val logger: Logger,
    private val immediateExecution: BittensorImmediateExecution,
    private val costBasisInteractor: BittensorCostBasisInteractor,
    private val chainState: ChainStateRepository
) {

    private val options = ExtrinsicService.SubmissionOptions(batchMode = BatchMode.BATCH_ALL)

    fun observe(metaId: Long, netuid: BittensorNetuid, hotkeyAddress: String): Flow<BittensorExitPosition> = flowOfAll {
        val initial = load(metaId, netuid, hotkeyAddress)
        var subnet = initial.subnet
        val snapshots = flowOfAll { chainState.currentRemoteBlockNumberFlow(initial.chain.id) }
            .conflate()
            .mapNotNull {
                requireWallet(metaId, initial.coldkey, initial.chain)
                if (subnet == null && !netuid.isRoot()) subnet = loadSubnetMetadata(netuid)
                refreshPreflight(initial, netuid)?.let { it to subnet }
            }
            .onStart { emit(initial.preflight to initial.subnet) }
            // Price drift alone must not restart the quote and fee of an unchanged position every block.
            .distinctUntilChanged { old, new -> old.first.refreshIdentity() == new.first.refreshIdentity() && old.second == new.second }
        combine(snapshots, walletRepository.assetFlow(metaId, initial.chain.utilityAsset)) { (preflight, subnet), asset ->
            initial.copy(preflight = preflight.withWithdrawalFloor(netuid), subnet = subnet, asset = asset)
        }
            .distinctUntilChanged()
    }

    suspend fun load(metaId: Long, netuid: BittensorNetuid, hotkeyAddress: String): BittensorExitPosition = coroutineScope {
        val wallet = accountRepository.getMetaAccount(metaId)
        immediateExecution.requireSupported(wallet)
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        val coldkey = AccountIdKey(wallet.requireAccountIdIn(chain))
        val hotkey = AccountIdKey(chain.accountIdOf(hotkeyAddress))
        // Optional display metadata loads alongside the chain snapshot instead of after it.
        val subnet = async { loadSubnetMetadata(netuid) }
        val preflight = exitRuntimeSource.preflight(
            chain.id,
            coldkey.value,
            hotkey.value,
            netuid
        )
        val asset = walletRepository.assetFlow(metaId, chain.utilityAsset)
            .first()
        val directSigner = wallet.type != LightMetaAccount.Type.PROXIED && wallet.type != LightMetaAccount.Type.MULTISIG
        requireWallet(metaId, coldkey, chain)
        BittensorExitPosition(
            metaId = metaId,
            chain = chain,
            coldkey = coldkey,
            hotkey = hotkey,
            validatorAddress = hotkeyAddress,
            netuid = netuid,
            asset = asset,
            subnet = subnet.await(),
            preflight = preflight.withWithdrawalFloor(netuid),
            supportsFeeFromStake = directSigner && !preflight.rootHoldEnabled,
            existentialDeposit = walletConstants.existentialDeposit(chain.id)
        )
    }

    fun maximumAmount(position: BittensorExitPosition, slippageFraction: BigDecimal = slippage.fraction): BigInteger {
        return position.preflight.withWithdrawalFloor(position.netuid, slippageFraction = slippageFraction).maximumAmount
    }

    suspend fun quote(
        position: BittensorExitPosition,
        amount: BigInteger,
        fullExit: Boolean,
        slippageFraction: BigDecimal = slippage.fraction
    ): BittensorExitQuote {
        require(amount.isPositive() && amount.bitLength() <= 64)
        val exitAmount = if (fullExit) position.preflight.totalStake else amount
        val quote = when (val netuid = position.netuid) {
            BittensorNetuid.Root -> rootQuote(exitAmount, fullExit)
            is BittensorNetuid.Subnet -> subnetQuote(
                position.chain,
                netuid,
                exitAmount,
                fullExit,
                slippageFraction
            )
        }
        val basis = position.preflight.withWithdrawalFloor(position.netuid, quote.spotPrice, slippageFraction)
        val plan = basis.plan(exitAmount, position.hotkey)
        val exitHotkeys = if (fullExit) {
            position.preflight.orderedHotkeys.ifEmpty { listOf(position.hotkey) }
        } else {
            emptyList()
        }
        return quote.copy(
            exitHotkeys = exitHotkeys,
            plan = plan,
            slippageFraction = slippageFraction,
            groupHotkeys = position.preflight.groupStakes.keys.toSet()
        )
    }

    suspend fun estimateFee(position: BittensorExitPosition, quote: BittensorExitQuote): Fee {
        // iOS estimates the gross request once. Preparation applies the loaded fee to a
        // stake-funded root partial; it does not feed that adjustment back into estimation.
        return estimateFee(position, operation(position, quote))
    }

    suspend fun refreshQuote(position: BittensorExitPosition, reviewed: BittensorExitQuote): BittensorExitQuote {
        if (position.isRoot) return reviewed
        return subnetQuote(position.chain, position.netuid as BittensorNetuid.Subnet, reviewed.amount, reviewed.fullExit, reviewed.slippageFraction)
            .copy(plan = reviewed.plan, exitHotkeys = reviewed.exitHotkeys, groupHotkeys = reviewed.groupHotkeys)
    }

    fun prepare(
        position: BittensorExitPosition,
        quote: BittensorExitQuote,
        fee: Fee,
        feeFromStake: Boolean
    ): BittensorExitDraft {
        val rootFee = if (position.isRoot && feeFromStake && quote.plan?.isBatched != true && quote.exitHotkeys.size <= 1) {
            fee.maxAmountDeductionFor(position.asset.token.configuration)
        } else {
            BigInteger.ZERO
        }
        return BittensorExitDraft(
            position,
            quote,
            { operation(position, quote, rootFee) },
            fee,
            feeFromStake
        )
    }

    suspend fun validationPayload(
        position: BittensorExitPosition,
        quote: BittensorExitQuote,
        fee: Fee,
        expectedFeeFromStake: Boolean? = null
    ): BittensorExitValidationPayload {
        requireWallet(position.metaId, position.coldkey, position.chain)
        val preflight = exitRuntimeSource.preflight(
            position.chain.id,
            position.coldkey.value,
            position.hotkey.value,
            position.netuid
        )
        val asset = walletRepository.assetFlow(position.metaId, position.chain.utilityAsset)
            .first()
        val existentialDeposit = walletConstants.existentialDeposit(position.chain.id)
        val latestQuote = if (position.isRoot) {
            null
        } else {
            subnetQuote(
                position.chain,
                position.netuid as BittensorNetuid.Subnet,
                quote.amount,
                quote.fullExit,
                quote.slippageFraction
            )
        }
        return BittensorExitValidationPayload(
            preflight = preflight,
            quote = quote,
            transferable = asset.transferableInPlanks,
            fee = fee.maxAmountDeductionFor(asset.token.configuration),
            existentialDeposit = existentialDeposit,
            supportsFeeFromStake = position.supportsFeeFromStake && !preflight.rootHoldEnabled &&
                quote.exitHotkeys.size <= 1 && quote.plan?.isBatched != true,
            expectedFeeFromStake = expectedFeeFromStake,
            latestQuote = latestQuote
        )
    }

    fun feeFromStake(position: BittensorExitPosition, fee: Fee): Boolean {
        return position.asset.transferableInPlanks < fee.maxAmountDeductionFor(position.asset.token.configuration) + position.existentialDeposit
    }

    suspend fun execute(draft: BittensorExitDraft): BittensorOperationResult {
        val position = draft.position
        requireWallet(position.metaId, position.coldkey, position.chain)
        // Submit the exact reviewed amount, price limit and service-fee transfer.
        val historyRequest = if (position.isRoot) null else costBasisInteractor.ownTradeRequest(position.metaId, position.chain)
        val result = submissionExecutor.execute(
            position.metaId,
            position.chain,
            draft.operation.outcomeTarget()
        ) { draft.operation.build(this) }
        costBasisInteractor.onTradeResult(historyRequest, result)
        return result
    }

    private fun operation(
        position: BittensorExitPosition,
        quote: BittensorExitQuote,
        rootFeeFromStake: BigInteger = BigInteger.ZERO
    ): BittensorExitOperation {
        require(quote.netuid == position.netuid) { "Withdrawal network does not match the position" }
        val beneficiary = AccountIdKey(position.chain.accountIdOf(beneficiaryAddress))
        return BittensorExitOperation(quote, position.hotkey, beneficiary, rootFeeFromStake)
    }

    private suspend fun loadSubnetMetadata(netuid: BittensorNetuid): BittensorSubnet? {
        return when (netuid) {
            BittensorNetuid.Root -> null
            is BittensorNetuid.Subnet -> try {
                subnetMetadataInteractor.subnet(netuid)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.log("[BittensorExit] Subnet metadata unavailable")
                logger.log(e)
                null
            }
        }
    }

    /** Background refresh: a failed read keeps the last snapshot; validation always re-reads the chain. */
    private suspend fun refreshPreflight(initial: BittensorExitPosition, netuid: BittensorNetuid): BittensorExitPreflight? {
        return try {
            exitRuntimeSource.preflight(initial.chain.id, initial.coldkey.value, initial.hotkey.value, netuid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.log("[BittensorExit] Background preflight refresh failed")
            logger.log(e)
            null
        }
    }

    private fun BittensorExitPreflight.withWithdrawalFloor(
        netuid: BittensorNetuid,
        price: BigInteger? = withdrawalPrice,
        slippageFraction: BigDecimal = slippage.fraction
    ): BittensorExitPreflight {
        val marginalPrice = price ?: return this
        val selectedSlippage = BittensorSlippage(slippageFraction)
        val limit = if (netuid.isRoot()) marginalPrice else selectedSlippage.limitPrice(marginalPrice, BittensorSwapDirection.ALPHA_TO_TAO)
        return copy(withdrawalFloor = bittensorWithdrawalFloor(minStake, limit, swapFeeRate))
    }

    private fun rootQuote(amount: BigInteger, fullExit: Boolean): BittensorExitQuote = BittensorExitQuote(
        netuid = BittensorNetuid.Root,
        amount = amount,
        fullExit = fullExit,
        estimatedTaoOut = amount,
        minimumTaoOut = amount,
        serviceFee = BigInteger.ZERO,
        limitPrice = null
    )

    private suspend fun subnetQuote(
        chain: Chain,
        subnetId: BittensorNetuid.Subnet,
        amount: BigInteger,
        fullExit: Boolean,
        slippageFraction: BigDecimal
    ): BittensorExitQuote {
        val runtimeQuote = swapSource.quote(
            chain.id,
            subnetId,
            BittensorSwapDirection.ALPHA_TO_TAO,
            amount
        )
        val simulation = runtimeQuote.simulation
        require(simulation.direction == BittensorSwapDirection.ALPHA_TO_TAO && simulation.subnetId == subnetId)
        require(simulation.amountIn + simulation.alphaFee == amount && simulation.amountOut.isPositive())

        val spotPrice = runtimeQuote.spotPrice
        val selectedSlippage = BittensorSlippage(slippageFraction)
        val limitPrice = selectedSlippage.limitPrice(spotPrice, BittensorSwapDirection.ALPHA_TO_TAO)
        val minimumTaoOut = simulation.amountIn * limitPrice / BittensorProtocolUnits.RAO_PER_TAO
        val commission = configRepository.config().commission
        val serviceFee = commission.feeFor(simulation.amountOut)
        require(minimumTaoOut.isPositive() && simulation.amountOut >= minimumTaoOut)

        return BittensorExitQuote(
            netuid = subnetId,
            amount = amount,
            fullExit = fullExit,
            estimatedTaoOut = simulation.amountOut,
            minimumTaoOut = minimumTaoOut,
            serviceFee = serviceFee,
            limitPrice = limitPrice,
            spotPrice = spotPrice,
            simulatedInput = simulation.amountIn,
            slippageFraction = slippageFraction,
            serviceFeeRate = commission.fraction
        )
    }

    private suspend fun estimateFee(position: BittensorExitPosition, operation: BittensorExitOperation): Fee {
        return extrinsicService.estimateFee(
            position.chain,
            TransactionOrigin.WalletWithId(position.metaId),
            options
        ) { operation.build(this) }
    }

    private suspend fun requireWallet(
        metaId: Long,
        coldkey: AccountIdKey,
        chain: Chain
    ) {
        val wallet = accountRepository.getMetaAccount(metaId)
        immediateExecution.requireSupported(wallet)
        check(
            wallet.id == metaId && wallet.requireAccountIdIn(chain).contentEquals(coldkey.value)
        ) { "The staking wallet has changed" }
    }
}
