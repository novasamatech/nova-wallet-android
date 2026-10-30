package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model

sealed class BittensorSubmissionState(val result: BittensorStakeResultUi?) {

    data object Ready : BittensorSubmissionState(null)

    class Submitting(result: BittensorStakeResultUi.Progress) : BittensorSubmissionState(result)

    class Completed(result: BittensorStakeResultUi.Success? = null) : BittensorSubmissionState(result)

    class Failed(result: BittensorStakeResultUi.Failure? = null) : BittensorSubmissionState(result)

    class Unconfirmed(val hash: String?, result: BittensorStakeResultUi.Unconfirmed? = null) : BittensorSubmissionState(result)

    /** The operation was executed or may have been: the screen can only be left, never resubmitted. */
    val isFinished: Boolean
        get() = this is Completed || this is Unconfirmed

    val canSubmit: Boolean
        get() = this is Ready || this is Failed

    fun dismissResult(): BittensorSubmissionState = when (this) {
        is Completed -> Completed()
        is Failed -> Failed()
        is Unconfirmed -> Unconfirmed(hash)
        else -> this
    }
}
