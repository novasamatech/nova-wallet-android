package io.novafoundation.nova.feature_staking_impl.domain.validations.unbond

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError

class EnoughToUnbondValidation : UnbondValidation {

    override suspend fun validate(value: UnbondValidationPayload): ValidationStatus<UnbondValidationFailure> {
        return validOrError(value.amount <= value.asset.bonded) {
            UnbondValidationFailure.NotEnoughBonded
        }
    }
}
