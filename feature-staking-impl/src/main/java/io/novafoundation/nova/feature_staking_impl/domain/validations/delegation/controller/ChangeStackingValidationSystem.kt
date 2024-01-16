package io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.controller

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.validations.AccountRequiredValidation

class ChangeStackingValidationPayload(
    val controllerAddress: String
)

enum class ChangeStackingValidationFailure {
    NO_ACCESS_TO_CONTROLLER_ACCOUNT
}

typealias ControllerRequiredValidation = AccountRequiredValidation<ChangeStackingValidationPayload, ChangeStackingValidationFailure>

typealias ChangeStackingValidationSystem = ValidationSystem<ChangeStackingValidationPayload, ChangeStackingValidationFailure>
typealias ChangeStackingValidationSystemBuilder = ValidationSystemBuilder<ChangeStackingValidationPayload, ChangeStackingValidationFailure>

fun ChangeStackingValidationSystemBuilder.controllerAccountAccess(accountRepository: AccountRepository, stakingSharedState: StakingSharedState) {
    return validate(
        ControllerRequiredValidation(
            accountRepository = accountRepository,
            accountAddressExtractor = { it.controllerAddress },
            sharedState = stakingSharedState,
            errorProducer = { ChangeStackingValidationFailure.NO_ACCESS_TO_CONTROLLER_ACCOUNT }
        )
    )
}
