package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.nominationPools

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrWarning
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationSystemBuilder

class ActivePoolValidation : StartMultiStakingValidation {

    override suspend fun validate(value: StartMultiStakingValidationPayload): ValidationStatus<StartMultiStakingValidationFailure> {
        val isPoolActive = value.selection.apy != null

        return isPoolActive isTrueOrWarning {
            StartMultiStakingValidationFailure.InactivePool
        }
    }
}

fun StartMultiStakingValidationSystemBuilder.activePool() {
    validate(ActivePoolValidation())
}
