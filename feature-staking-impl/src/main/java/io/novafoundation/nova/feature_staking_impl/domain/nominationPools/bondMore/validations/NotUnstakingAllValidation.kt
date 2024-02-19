package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novasama.substrate_sdk_android.hash.isPositive

class NotUnstakingAllValidation : NominationPoolsBondMoreValidation {

    override suspend fun validate(value: NominationPoolsBondMoreValidationPayload): ValidationStatus<NominationPoolsBondMoreValidationFailure> {
        val activePoints = value.poolMember.points.value

        return activePoints.isPositive() isTrueOrError {
            NominationPoolsBondMoreValidationFailure.UnstakingAll
        }
    }
}

fun NominationPoolsBondMoreValidationSystemBuilder.notUnstakingAll() {
    validate(NotUnstakingAllValidation())
}
