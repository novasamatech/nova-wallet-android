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
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.map
import io.novafoundation.nova.feature_staking_impl.data.bittensor.bittensorFloorsClose
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.rootHoldBlocks
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.time.Duration.Companion.milliseconds

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

    private companion object {

        const val SUBNET_METADATA_ATTEMPTS = 5

        const val BLOCKS_RESUBSCRIBE_DELAY_MILLIS = 5_000L
    }

    private val options = ExtrinsicService.SubmissionOptions(batchMode = BatchMode.BATCH_ALL)

    /** The position refreshed on each new block while [shown]; the load already read the current block. */
    fun observe(
        metaId: Long,
        netuid: BittensorNetuid,
        hotkeyAddress: String,
        shown: Flow<Boolean> = flowOf(true)
    ): Flow<BittensorExitPosition> = flowOfAll {
        val initial = load(metaId, netuid, hotkeyAddress)
        var subnet = initial.subnet
        val metadataRetry = BittensorBlockBackoff(SUBNET_METADATA_ATTEMPTS)
        val snapshots = newBlocks(initial.chain.id, shown)
            .mapNotNull {
                requireWallet(metaId, initial.coldkey, initial.chain)
                if (subnet == null && !netuid.isRoot() && metadataRetry.shouldAttempt()) subnet = loadSubnetMetadata(netuid)
                refreshPreflight(initial, netuid)?.let { it to subnet }
            }
            .onStart { emit(initial.preflight to initial.subnet) }
            .map { (preflight, subnet) -> preflight.withWithdrawalFloor(netuid) to subnet }
            // Price drift alone must not restart the quote and fee of an unchanged position every block;
            // the Max floor is refreshed once it moves by more than the bucket.
            .distinctUntilChanged { old, new ->
                old.second == new.second && old.first.refreshIdentity() == new.first.refreshIdentity() &&
                    bittensorFloorsClose(old.first.withdrawalFloor, new.first.withdrawalFloor)
            }
        combine(snapshots, walletRepository.assetFlow(metaId, initial.chain.utilityAsset)) { (preflight, subnet), asset ->
            initial.copy(preflight = preflight, subnet = subnet, asset = asset)
        }
            .distinctUntilChanged()
    }

    /** Each block after the current one while [shown]; a hidden screen skips them and resumes with the next block. */
    fun newBlocks(chainId: ChainId, shown: Flow<Boolean>): Flow<Unit> = shown.distinctUntilChanged()
        .flatMapLatest { visible ->
            if (visible) flowOfAll { chainState.currentRemoteBlockNumberFlow(chainId) }.drop(1).map { } else emptyFlow()
        }
        .conflate()

    /** What re-quotes a sale: each new block while [shown] and each of [immediate]; a lost block subscription is restored after a short pause. */
    fun requotes(shown: Flow<Boolean>, immediate: Flow<Unit>): Flow<Unit> = merge(newBlocks(ChainGeneses.BITTENSOR, shown), immediate)
        .retryWhen { cause, _ ->
            if (cause is CancellationException) throw cause
            delay(BLOCKS_RESUBSCRIBE_DELAY_MILLIS)
            true
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

    /** The position's group with the floors of the selected slippage, from the same snapshot as Max and the plan. */
    fun exitGroup(position: BittensorExitPosition, slippageFraction: BigDecimal = slippage.fraction): BittensorExitGroup {
        return position.preflight.withWithdrawalFloor(position.netuid, slippageFraction).exitGroup(position.hotkey)
    }

    fun maximumAmount(
        position: BittensorExitPosition,
        selection: BittensorExitSelection? = null,
        slippageFraction: BigDecimal = slippage.fraction
    ): BigInteger {
        val group = exitGroup(position, slippageFraction)
        return group.maximum(group.resolve(selection))
    }

    /**
     * Quotes the plan for [amount] within the selected validators; the plan may settle on a nearby smaller amount, which
     * is the one quoted. A plan that empties the whole group is the full exit.
     */
    suspend fun quote(
        position: BittensorExitPosition,
        amount: BigInteger,
        selection: BittensorExitSelection? = null,
        slippageFraction: BigDecimal = slippage.fraction
    ): BittensorExitQuote {
        require(amount.isPositive() && amount.bitLength() <= 64)
        val group = exitGroup(position, slippageFraction)
        val plan = group.basis(group.resolve(selection)).plan(amount, group.floors) ?: throw BittensorExitExceedsStakeException()
        val fullExit = plan.emptiesGroup(group.hotkeys)
        val quote = when (val netuid = position.netuid) {
            BittensorNetuid.Root -> rootQuote(plan.amount, fullExit)
            is BittensorNetuid.Subnet -> subnetQuote(
                position.chain,
                netuid,
                plan.amount,
                fullExit,
                slippageFraction
            )
        }
        return quote.copy(
            exitHotkeys = if (fullExit) group.hotkeys else emptyList(),
            plan = plan,
            slippageFraction = slippageFraction,
            groupHotkeys = position.preflight.groupStakes.keys.toSet()
        )
    }

    /** The root hold of one group validator, for the hold explanation; null when it is not held. */
    suspend fun rootHold(position: BittensorExitPosition, hotkey: AccountIdKey): BittensorRootHold? {
        val remaining = position.preflight.groupMembers[hotkey]?.holdRemaining?.takeIf { it.isPositive() } ?: return null
        val blockMillis = chainState.expectedBlockTimeInMillis(position.chain.id)
        return BittensorRootHold(remaining * blockMillis, position.preflight.rootHoldInterval * blockMillis)
    }

    /** The maximum of [selection] and the root hold left, from the chain state the validation read, for the failure explanations. */
    suspend fun failureLimits(
        payload: BittensorExitValidationPayload,
        position: BittensorExitPosition,
        selection: BittensorExitSelection
    ): BittensorExitFailureLimits {
        val maximum = maximumAmount(position.copy(preflight = payload.preflight), selection, payload.quote.slippageFraction)
        val holdDuration = payload.rootHoldBlocks().takeIf { it.isPositive() }
            ?.let { (it * chainState.expectedBlockTimeInMillis(position.chain.id)).toLong().milliseconds }
        return BittensorExitFailureLimits(maximum, holdDuration)
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

    /** A block re-quote of the sale; a failed or timed-out read fails it, as iOS clears a failed quote. */
    suspend fun blockRequote(position: BittensorExitPosition, reviewed: BittensorExitQuote): Result<BittensorExitQuote> {
        return try {
            Result.success(refreshQuote(position, reviewed))
        } catch (cause: Exception) {
            if (cause is CancellationException && cause !is TimeoutCancellationException) throw cause
            logger.log("[BittensorExit] Block re-quote failed")
            logger.log(cause)
            Result.failure(cause)
        }
    }

    /** The draft with the fee taken from the stake when the free balance cannot pay it. */
    fun prepare(position: BittensorExitPosition, quote: BittensorExitQuote, fee: Fee): BittensorExitDraft {
        return prepare(position, quote, fee, feeFromStake(position, fee))
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
        // The re-read quote keeps the plan, so a confirmed rate update or the signature can take it as it is.
        val latestQuote = if (position.isRoot) null else refreshQuote(position, quote)
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
        slippageFraction: BigDecimal = slippage.fraction
    ): BittensorExitPreflight {
        val marginalPrice = withdrawalPrice ?: return this
        val selectedSlippage = BittensorSlippage(slippageFraction)
        val limit = if (netuid.isRoot()) marginalPrice else selectedSlippage.limitPrice(marginalPrice, BittensorSwapDirection.ALPHA_TO_TAO)
        return copy(
            withdrawalFloor = bittensorWithdrawalFloor(minStake, limit, swapFeeRate),
            sweepFloor = bittensorWithdrawalFloor(minNominatorStake, limit, BigInteger.ZERO)
        )
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
        // As on iOS, a sale that its own impact cannot fill at the limit price is still quoted; validation blocks it.
        require(minimumTaoOut.isPositive())

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

/** Exponential backoff over block ticks: attempts on ticks 1, 3, 7, 15, … and gives up after [maxAttempts]. */
internal class BittensorBlockBackoff(private val maxAttempts: Int) {

    private var tick = 0L

    private var attempts = 0

    private var nextTick = 1L

    fun shouldAttempt(): Boolean {
        tick += 1
        if (attempts >= maxAttempts || tick < nextTick) return false
        attempts += 1
        nextTick = tick + (1L shl attempts)
        return true
    }
}
