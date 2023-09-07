package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount

typealias StartMultiStakingValidation = Validation<StartMultiStakingValidationPayload, StartMultiStakingValidationFailure>
typealias StartMultiStakingValidationSystem = ValidationSystem<StartMultiStakingValidationPayload, StartMultiStakingValidationFailure>
typealias StartMultiStakingValidationSystemBuilder = ValidationSystemBuilder<StartMultiStakingValidationPayload, StartMultiStakingValidationFailure>

fun StartMultiStakingValidationSystemBuilder.positiveBond() {
    positiveAmount(
        amount = { it.amountOf(it.selection.stake) },
        error = { StartMultiStakingValidationFailure.NonPositiveAmount }
    )
}
