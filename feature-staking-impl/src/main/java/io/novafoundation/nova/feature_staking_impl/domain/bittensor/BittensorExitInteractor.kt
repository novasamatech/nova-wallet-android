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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

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
    private val logger: Logger
) {

    private val options = ExtrinsicService.SubmissionOptions(batchMode = BatchMode.BATCH_ALL)

    suspend fun load(metaId: Long, netuid: BittensorNetuid, hotkeyAddress: String): BittensorExitPosition {
        val wallet = accountRepository.getMetaAccount(metaId)
        requireBittensorImmediateWallet(wallet)
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        val coldkey = AccountIdKey(wallet.requireAccountIdIn(chain))
        val hotkey = AccountIdKey(chain.accountIdOf(hotkeyAddress))
        val preflight = exitRuntimeSource.preflight(
            chain.id,
            coldkey.value,
            hotkey.value,
            netuid
        )
        val asset = walletRepository.assetFlow(metaId, chain.utilityAsset)
            .first()
        val subnet = loadSubnetMetadata(netuid)
        val directSigner = wallet.type != LightMetaAccount.Type.PROXIED && wallet.type != LightMetaAccount.Type.MULTISIG
        requireWallet(metaId, coldkey, chain)
        return BittensorExitPosition(
            metaId = metaId,
            chain = chain,
            coldkey = coldkey,
            hotkey = hotkey,
            validatorAddress = hotkeyAddress,
            netuid = netuid,
            asset = asset,
            subnet = subnet,
            preflight = preflight,
            supportsFeeFromStake = directSigner && !preflight.rootHoldEnabled,
            existentialDeposit = walletConstants.existentialDeposit(chain.id)
        )
    }

    suspend fun quote(position: BittensorExitPosition, amount: BigInteger, fullExit: Boolean): BittensorExitQuote {
        require(amount.isPositive() && amount.bitLength() <= 64)
        val exitAmount = if (fullExit) position.preflight.stake else amount
        val quote = when (val netuid = position.netuid) {
            BittensorNetuid.Root -> rootQuote(exitAmount, fullExit)
            is BittensorNetuid.Subnet -> subnetQuote(
                position.chain,
                netuid,
                exitAmount,
                fullExit
            )
        }
        return quote
    }

    suspend fun estimateFee(position: BittensorExitPosition, quote: BittensorExitQuote): Fee {
        return estimateFee(position, operation(position, quote))
    }

    fun prepare(
        position: BittensorExitPosition,
        quote: BittensorExitQuote,
        fee: Fee,
        feeFromStake: Boolean
    ): BittensorExitDraft {
        return BittensorExitDraft(
            position,
            quote,
            operation(position, quote),
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
        return BittensorExitValidationPayload(
            preflight = preflight,
            quote = quote,
            transferable = asset.transferableInPlanks,
            fee = fee.maxAmountDeductionFor(asset.token.configuration),
            existentialDeposit = existentialDeposit,
            supportsFeeFromStake = position.supportsFeeFromStake && !preflight.rootHoldEnabled,
            expectedFeeFromStake = expectedFeeFromStake
        )
    }

    fun feeFromStake(position: BittensorExitPosition, fee: Fee): Boolean {
        return position.asset.transferableInPlanks < fee.maxAmountDeductionFor(position.asset.token.configuration) + position.existentialDeposit
    }

    suspend fun execute(draft: BittensorExitDraft): BittensorOperationResult {
        val position = draft.position
        requireWallet(position.metaId, position.coldkey, position.chain)
        // Submit the exact reviewed amount, price limit and service-fee transfer.
        return submissionExecutor.execute(
            position.metaId,
            position.chain
        ) { draft.operation.build(this) }
    }

    private fun operation(position: BittensorExitPosition, quote: BittensorExitQuote): BittensorExitOperation {
        require(quote.netuid == position.netuid) { "Withdrawal network does not match the position" }
        val beneficiary = AccountIdKey(position.chain.accountIdOf(beneficiaryAddress))
        return BittensorExitOperation(quote, position.hotkey, beneficiary)
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
        fullExit: Boolean
    ): BittensorExitQuote {
        val simulation = swapSource.simulate(
            chain.id,
            subnetId,
            BittensorSwapDirection.ALPHA_TO_TAO,
            amount
        )
        require(simulation.direction == BittensorSwapDirection.ALPHA_TO_TAO && simulation.subnetId == subnetId)
        require(simulation.amountIn + simulation.alphaFee == amount && simulation.amountOut.isPositive())

        val spotPrice = swapSource.currentAlphaPrice(chain.id, subnetId)
        val limitPrice = slippage.limitPrice(spotPrice, BittensorSwapDirection.ALPHA_TO_TAO)
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
            limitPrice = limitPrice
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
        requireBittensorImmediateWallet(wallet)
        check(
            wallet.id == metaId && wallet.requireAccountIdIn(chain).contentEquals(coldkey.value)
        ) { "The staking wallet has changed" }
    }
}
