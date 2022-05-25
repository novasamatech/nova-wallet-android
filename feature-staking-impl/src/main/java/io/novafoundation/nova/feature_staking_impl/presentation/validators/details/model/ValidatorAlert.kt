package io.novafoundation.nova.feature_staking_impl.presentation.validators.details.model
import androidx.annotation.StringRes
import io.novafoundation.nova.feature_staking_impl.R

sealed class ValidatorAlert(@StringRes val descriptionRes: Int, val severity: Severity) {

    enum class Severity {
        WARNING, ERROR
    }

    sealed class Oversubscribed(@StringRes errorDescription: Int) : ValidatorAlert(errorDescription, Severity.WARNING) {

        object UserNotInvolved : ValidatorAlert.Oversubscribed(R.string.staking_validator_other_oversubscribed_message)

        class UserMissedReward(errorDescription: Int) : ValidatorAlert.Oversubscribed(errorDescription)
    }

    object Slashed : ValidatorAlert(R.string.staking_validator_slashed_desc, Severity.ERROR)
}
