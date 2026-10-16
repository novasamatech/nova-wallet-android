package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novasama.substrate_sdk_android.hash.isPositive
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorConfigRepository
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitRuntimeSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSwapSource
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.math.BigInteger

class BittensorExitInteractor(
    private val chains: ChainRegistry,
    private val accounts: AccountRepository,
    private val wallets: WalletRepository,
    private val source: BittensorExitRuntimeSource,
    private val subnets: BittensorSubnetMetadataInteractor,
    private val swaps: BittensorSwapSource,
    private val config: BittensorConfigRepository,
    private val slippage: BittensorSlippage,
    private val extrinsics: ExtrinsicService,
    private val submission: BittensorSubmissionExecutor,
    private val constants: WalletConstants,
    private val beneficiaryAddress: String,
    private val logger: Logger
) {

    private val options = ExtrinsicService.SubmissionOptions(batchMode = BatchMode.BATCH_ALL)

    suspend fun load(metaId: Long, netuid: BittensorNetuid, hotkeyAddress: String): BittensorExitPosition {
        val wallet = accounts.getSelectedMetaAccount()
        requireBittensorImmediateWallet(wallet)
        check(wallet.id == metaId) { "The staking wallet has changed" }
        val chain = chains.getChain(ChainGeneses.BITTENSOR)
        val coldkey = AccountIdKey(wallet.requireAccountIdIn(chain))
        val hotkey = AccountIdKey(chain.accountIdOf(hotkeyAddress))
        val preflight = source.preflight(chain.id, coldkey.value, hotkey.value, netuid)
        val asset = wallets.assetFlow(metaId, chain.utilityAsset).first()
        val subnet = if (netuid.isRoot()) {
            null
        } else {
            try {
                subnets.subnet(netuid as BittensorNetuid.Subnet)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.log("[BittensorExit] Subnet metadata unavailable")
                logger.log(e)
                null
            }
        }
        val directSigner = wallet.type != LightMetaAccount.Type.PROXIED && wallet.type != LightMetaAccount.Type.MULTISIG
        requireWallet(metaId, coldkey, chain)
        return BittensorExitPosition(
            metaId, chain, coldkey, hotkey, hotkeyAddress, netuid, asset, subnet, preflight,
            supportsFeeFromStake = directSigner && !preflight.rootHoldEnabled
        )
    }

    suspend fun quote(position: BittensorExitPosition, amount: BigInteger, fullExit: Boolean): BittensorExitQuote {
        require(amount.isPositive() && amount.bitLength() <= 64)
        val actual = if (fullExit) position.preflight.stake else amount
        val quote = if (position.isRoot) {
            BittensorExitQuote(BittensorNetuid.Root, actual, fullExit, actual, actual, BigInteger.ZERO, null)
        } else {
            val subnetId = BittensorSubnetId(position.netuid.value)
            val simulated = swaps.simulate(position.chain.id, subnetId, BittensorSwapDirection.ALPHA_TO_TAO, actual)
            require(simulated.direction == BittensorSwapDirection.ALPHA_TO_TAO && simulated.subnetId == subnetId)
            require(simulated.amountIn + simulated.alphaFee == actual && simulated.amountOut.isPositive())
            val price = swaps.currentAlphaPrice(position.chain.id, subnetId)
            val limit = slippage.limitPrice(price, BittensorSwapDirection.ALPHA_TO_TAO)
            val minimum = simulated.amountIn * limit / BittensorProtocolUnits.RAO_PER_TAO
            val commission = config.config().commission
            val serviceFee = commission.feeFor(simulated.amountOut)
            require(minimum.isPositive() && simulated.amountOut >= minimum)
            BittensorExitQuote(position.netuid, actual, fullExit, simulated.amountOut, minimum, serviceFee, limit)
        }
        validateBittensorExit(position.preflight, quote)
        return quote
    }

    suspend fun prepare(position: BittensorExitPosition, quote: BittensorExitQuote): BittensorExitDraft {
        requireWallet(position.metaId, position.coldkey, position.chain)
        require(position.netuid == quote.netuid)
        validateBittensorExit(position.preflight, quote)
        val beneficiary = AccountIdKey(position.chain.accountIdOf(beneficiaryAddress))
        val operation = BittensorExitOperation(quote, position.hotkey, beneficiary)
        val fee = extrinsics.estimateFee(position.chain, TransactionOrigin.WalletWithId(position.metaId), options) { operation.build(this) }
        val fromStake = checkFee(position, quote, fee)
        requireWallet(position.metaId, position.coldkey, position.chain)
        return BittensorExitDraft(position, quote, operation, fee, fromStake)
    }

    suspend fun execute(draft: BittensorExitDraft): BittensorOperationResult {
        val position = draft.position
        requireWallet(position.metaId, position.coldkey, position.chain)
        val current = source.preflight(position.chain.id, position.coldkey.value, position.hotkey.value, position.netuid)
        validateBittensorExit(current, draft.quote)
        val fee = extrinsics.estimateFee(position.chain, TransactionOrigin.WalletWithId(position.metaId), options) { draft.operation.build(this) }
        val feeFromStake = checkFee(
            position.copy(preflight = current, supportsFeeFromStake = position.supportsFeeFromStake && !current.rootHoldEnabled),
            draft.quote,
            fee
        )
        require(feeFromStake == draft.feeFromStake) { "The source of the network fee changed. Review the withdrawal again" }
        requireWallet(position.metaId, position.coldkey, position.chain)
        // The confirmed amount, limit and service-fee transfer are never requoted before signing.
        return submission.execute(position.metaId, position.chain) { draft.operation.build(this) }
    }

    private suspend fun checkFee(position: BittensorExitPosition, quote: BittensorExitQuote, fee: Fee): Boolean {
        val asset = wallets.assetFlow(position.metaId, position.chain.utilityAsset).first()
        val deduction = fee.maxAmountDeductionFor(asset.token.configuration)
        val deposit = constants.existentialDeposit(position.chain.id)
        if (asset.transferableInPlanks >= deduction + deposit) return false
        require(position.isRoot && position.supportsFeeFromStake) { "Free balance is required for the network fee and minimum account balance" }
        require(quote.amount > deduction + deposit) { "Unstaked amount cannot cover the network fee and minimum account balance" }
        if (!quote.fullExit) {
            require(quote.amount + deduction <= position.preflight.stake) { "Stake cannot cover the amount and network fee" }
            val remainder = position.preflight.stake - quote.amount - deduction
            require(remainder == BigInteger.ZERO || position.preflight.isOwnHotkey || remainder >= position.preflight.minNominatorStake) {
                "The network fee would leave a stake below the chain minimum. Review a full withdrawal"
            }
        }
        return true
    }

    private suspend fun requireWallet(metaId: Long, coldkey: AccountIdKey, chain: Chain) {
        val wallet = accounts.getSelectedMetaAccount()
        requireBittensorImmediateWallet(wallet)
        check(wallet.id == metaId && wallet.requireAccountIdIn(chain).contentEquals(coldkey.value)) { "The staking wallet has changed" }
    }
}
