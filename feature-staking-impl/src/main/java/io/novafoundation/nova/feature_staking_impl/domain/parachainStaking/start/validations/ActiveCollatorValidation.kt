package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.isActive

class ActiveCollatorValidation : StartParachainStakingValidation {

    override suspend fun validate(value: StartParachainStakingValidationPayload): ValidationStatus<StartParachainStakingValidationFailure> {
        val candidateMetadata = value.collator.candidateMetadata

        return candidateMetadata.isActive isTrueOrError { StartParachainStakingValidationFailure.CollatorIsNotActive }
    }
}

fun ValidationSystemBuilder<StartParachainStakingValidationPayload, StartParachainStakingValidationFailure>.activeCollator() {
    validate(ActiveCollatorValidation())
}
