package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakePreflightRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
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
    /** The tolerance the user chose; as on iOS, the market is re-read at it before staking. */
    val slippage: BittensorSlippage? = null
)

/**
 * A stake check [failure] with what its alert names: the checked [quote], the [latestQuote] the market was re-read at
 * and the [maximumStake] the balance leaves after the network fee and the fee reserve.
 */
data class BittensorStakePreflightFailure(
    val failure: BittensorStakeValidationFailure,
    val quote: BittensorSubnetBuyQuote? = null,
    val latestQuote: BittensorSubnetBuyQuote? = null,
    val maximumStake: BigInteger? = null
)

typealias BittensorStakePreflightSystem = ValidationSystem<BittensorStakePreflightPayload, BittensorStakePreflightFailure>

class BittensorStakePreflightValidation(
    private val preflightRepository: BittensorStakePreflightRepository,
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val stakeValidationSystem: BittensorStakeValidationSystem,
    private val bittensorSubnetInteractor: BittensorSubnetInteractor,
    private val validatorRepository: BittensorSubnetValidatorRepository,
    private val immediateExecution: BittensorImmediateExecution,
    private val immediateValidation: BittensorImmediateStakeValidation
) : Validation<BittensorStakePreflightPayload, BittensorStakePreflightFailure> {

    override suspend fun validate(value: BittensorStakePreflightPayload): ValidationStatus<BittensorStakePreflightFailure> = coroutineScope {
        val account = accountRepository.getMetaAccount(value.metaId)
        val executionStatus = immediateValidation.validate(immediateExecution.support(account))
        if (executionStatus is ValidationStatus.NotValid) return@coroutineScope executionStatus.withAlertValues { BittensorStakePreflightFailure(it) }
        val preflight = async {
            preflightRepository.load(value.chain.id, value.netuid, account.requireAccountIdIn(value.chain), value.hotkey)
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
        val asset = walletRepository.assetFlow(value.metaId, value.chain.utilityAsset)
            .first()
        val currentQuote = latestQuote.await()
        val payload = BittensorStakeValidationPayload(
            asset = asset,
            amount = asset.token.amountFromPlanks(value.amount),
            fee = value.fee,
            preflight = preflight.await(),
            quote = value.quote,
            netuid = value.netuid,
            priceImpactAccepted = value.priceImpactAccepted,
            checkedAtMillis = System.currentTimeMillis(),
            latestQuote = currentQuote,
            inactiveValidator = inactiveValidator.await(),
            inactiveValidatorAccepted = value.inactiveValidatorAccepted
        )
        stakeValidationSystem.validate(payload)
            .getOrThrow()
            .withAlertValues { BittensorStakePreflightFailure(it, value.quote, currentQuote, maximumStake(asset, value.fee)) }
    }

    // As on iOS: a market that cannot be re-read leaves no quote to stake against, which the validations report.
    private suspend fun latestQuote(quote: BittensorSubnetBuyQuote, slippage: BittensorSlippage): BittensorSubnetBuyQuote? = try {
        bittensorSubnetInteractor.quote(quote.subnetId, quote.totalTaoInPlanks, slippage)
    } catch (cause: Exception) {
        if (cause is CancellationException && cause !is TimeoutCancellationException) throw cause
        null
    }

    private fun maximumStake(asset: Asset, fee: Fee): BigInteger {
        return maximumBittensorStake(asset.transferableInPlanks, fee.maxAmountDeductionFor(asset.token.configuration))
    }

    private fun ValidationStatus<BittensorStakeValidationFailure>.withAlertValues(
        values: (BittensorStakeValidationFailure) -> BittensorStakePreflightFailure
    ): ValidationStatus<BittensorStakePreflightFailure> = when (this) {
        is ValidationStatus.Valid -> valid()
        is ValidationStatus.NotValid -> ValidationStatus.NotValid(level, values(reason))
    }
}
