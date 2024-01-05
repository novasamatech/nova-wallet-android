package io.novafoundation.nova.feature_account_impl.domain.validation.proxy

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError

package io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

class AddProxyValidationPayload(
    val chain: Chain,
    val asset: Asset,
    val address: String,
    val fee: Fee,
    val deposit: Balance
)

sealed class AddProxyValidationFailure {

    class NotEnoughToPayFee(
        override val chainAsset: Chain.Asset,
        override val maxUsable: BigDecimal,
        override val fee: BigDecimal
    ) : AddProxyValidationFailure(), NotEnoughToPayFeesError

    class NotEnoughBalanceToReserveDeposit(
        val chainAsset: Chain.Asset,
        val maxUsable: Balance,
        val deposit: Balance
    ) : AddProxyValidationFailure()

    class InvalidAddress(val chain: Chain) : AddProxyValidationFailure()

}

typealias AddProxyValidationSystem = ValidationSystem<AddProxyValidationPayload, AddProxyValidationFailure>
typealias AddProxyValidationSystemBuilder = ValidationSystemBuilder<AddProxyValidationPayload, AddProxyValidationFailure>

fun AddProxyValidationSystemBuilder.controllerAccountAccess(accountRepository: AccountRepository, stakingSharedState: StakingSharedState) {
    return validate(
        ControllerRequiredValidation(
            accountRepository = accountRepository,
            accountAddressExtractor = { it.controllerAddress },
            sharedState = stakingSharedState,
            errorProducer = { ChangeStackingValidationFailure.NO_ACCESS_TO_CONTROLLER_ACCOUNT }
        )
    )
}
