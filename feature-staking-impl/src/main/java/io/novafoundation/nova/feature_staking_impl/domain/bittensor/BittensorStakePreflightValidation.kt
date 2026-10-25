package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BITTENSOR_REQUEST_TIMEOUT
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakePreflightRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorTimeoutException
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorImmediateStakeValidation

data class BittensorStakePreflightPayload(
    val chain: Chain,
    val metaId: Long,
    val netuid: BittensorNetuid,
    val hotkey: AccountId,
    val amount: BigInteger,
    val fee: Fee,
    val quote: BittensorSubnetBuyQuote? = null,
    val priceImpactAccepted: Boolean = false,
    val existingPosition: Boolean = false,
    val inactiveValidatorAccepted: Boolean = false,
    /** The tolerance the user chose; the market is re-read at it before staking. */
    val slippage: BittensorSlippage? = null
)

typealias BittensorStakePreflightSystem = ValidationSystem<BittensorStakePreflightPayload, BittensorStakeValidationFailure>

class BittensorStakePreflightValidation(
    private val preflightRepository: BittensorStakePreflightRepository,
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val stakeValidationSystem: BittensorStakeValidationSystem,
    private val bittensorSubnetInteractor: BittensorSubnetInteractor,
    private val validatorRepository: BittensorSubnetValidatorRepository,
    private val immediateExecution: BittensorImmediateExecution,
    private val immediateValidation: BittensorImmediateStakeValidation
) : Validation<BittensorStakePreflightPayload, BittensorStakeValidationFailure> {

    override suspend fun validate(value: BittensorStakePreflightPayload): ValidationStatus<BittensorStakeValidationFailure> = coroutineScope {
        val account = accountRepository.getMetaAccount(value.metaId)
        val executionStatus = immediateValidation.validate(immediateExecution.support(account))
        if (executionStatus is ValidationStatus.NotValid) return@coroutineScope executionStatus
        // An unreachable node must end as a slow-network failure the screen can offer to retry, not an endless spinner.
        val preflight = async {
            withTimeoutOrNull(BITTENSOR_REQUEST_TIMEOUT) {
                preflightRepository.load(value.chain.id, value.netuid, account.requireAccountIdIn(value.chain), value.hotkey)
            } ?: throw BittensorTimeoutException("Stake preflight")
        }
        val latestQuote = async {
            value.quote?.let { latestQuote(it, value.slippage ?: bittensorSubnetInteractor.slippage) }
        }
        val inactiveValidator = async {
            val netuid = value.netuid
            if (value.existingPosition && netuid is BittensorNetuid.Subnet) {
                val hotkey = AccountIdKey(value.hotkey)
                val validator = BittensorValidator(hotkey, value.chain.addressOf(hotkey))
                validatorRepository.existingPositionValidator(value.chain, netuid, validator).inactive
            } else {
                false
            }
        }
        val asset = withTimeoutOrNull(BITTENSOR_REQUEST_TIMEOUT) {
            walletRepository.assetFlow(value.metaId, value.chain.utilityAsset).first()
        } ?: throw BittensorTimeoutException("Stake balance")
        val loadedPreflight = preflight.await()
        val loadedLatestQuote = latestQuote.await()
        val loadedInactiveValidator = inactiveValidator.await()
        // Read the clock only after every read has finished: the re-read quote is stamped when it arrives,
        // so an earlier check time would make it look as if it was captured in the future.
        val checkedAtMillis = System.currentTimeMillis()
        val payload = BittensorStakeValidationPayload(
            asset = asset,
            amount = asset.token.amountFromPlanks(value.amount),
            fee = value.fee,
            preflight = loadedPreflight,
            quote = value.quote,
            netuid = value.netuid,
            priceImpactAccepted = value.priceImpactAccepted,
            checkedAtMillis = checkedAtMillis,
            latestQuote = loadedLatestQuote,
            inactiveValidator = loadedInactiveValidator,
            inactiveValidatorAccepted = value.inactiveValidatorAccepted
        )
        stakeValidationSystem.validate(payload).getOrThrow()
    }

    // A market that cannot be re-read leaves no quote to stake against, which the validations report.
    private suspend fun latestQuote(quote: BittensorSubnetBuyQuote, slippage: BittensorSlippage): BittensorSubnetBuyQuote? = try {
        bittensorSubnetInteractor.quote(quote.subnetId, quote.totalTaoInPlanks, slippage)
    } catch (cause: Exception) {
        if (cause is CancellationException && cause !is TimeoutCancellationException) throw cause
        null
    }
}
