package io.novafoundation.nova.feature_staking_impl.domain.validations.rebond

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError

class EnoughToRebondValidation : RebondValidation {

    override suspend fun validate(value: RebondValidationPayload): ValidationStatus<RebondValidationFailure> {
        return validOrError(value.rebondAmount <= value.controllerAsset.unbonding) {
            RebondValidationFailure.NotEnoughUnbondings
        }
    }
}
