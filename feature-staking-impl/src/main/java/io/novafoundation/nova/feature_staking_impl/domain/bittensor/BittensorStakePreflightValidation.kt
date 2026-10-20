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
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakePreflightRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger
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
    val inactiveValidatorAccepted: Boolean = false
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
        val executionStatus = immediateValidation.validate(immediateExecution.isSupported(account))
        if (executionStatus is ValidationStatus.NotValid) return@coroutineScope executionStatus
        val preflight = async {
            preflightRepository.load(value.chain.id, value.netuid, account.requireAccountIdIn(value.chain), value.hotkey)
        }
        val latestQuote = async {
            value.quote?.let { bittensorSubnetInteractor.quote(it.subnetId, it.totalTaoInPlanks) }
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
    }
}
