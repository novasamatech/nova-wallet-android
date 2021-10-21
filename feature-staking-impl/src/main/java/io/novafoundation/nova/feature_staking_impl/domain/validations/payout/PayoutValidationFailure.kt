package io.novafoundation.nova.feature_staking_impl.domain.validations.payout

sealed class PayoutValidationFailure {
    object CannotPayFee : PayoutValidationFailure()

    object UnprofitablePayout : PayoutValidationFailure()
}
