package io.novafoundation.nova.feature_staking_impl.domain.validations.payout

import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus

class ProfitablePayoutValidation : Validation<MakePayoutPayload, PayoutValidationFailure> {

    override suspend fun validate(value: MakePayoutPayload): ValidationStatus<PayoutValidationFailure> {
        return if (value.fee < value.totalReward) {
            ValidationStatus.Valid()
        } else {
            ValidationStatus.NotValid(DefaultFailureLevel.WARNING, reason = PayoutValidationFailure.UnprofitablePayout)
        }
    }
}
