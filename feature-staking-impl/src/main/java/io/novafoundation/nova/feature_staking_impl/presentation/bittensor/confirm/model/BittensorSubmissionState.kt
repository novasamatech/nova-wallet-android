package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model

sealed class BittensorSubmissionState(val result: BittensorStakeResultUi?) {

    data object Ready : BittensorSubmissionState(null)

    class Submitting(result: BittensorStakeResultUi.Progress) : BittensorSubmissionState(result)

    class Completed(result: BittensorStakeResultUi.Success? = null) : BittensorSubmissionState(result)

    class Failed(result: BittensorStakeResultUi.Failure? = null) : BittensorSubmissionState(result)

    class Unresolved(val hash: String, result: BittensorStakeResultUi.Failure? = null) : BittensorSubmissionState(result)

    class Recovered(val recovery: BittensorStakeResultUi.Recovered, showResult: Boolean = true) :
        BittensorSubmissionState(recovery.takeIf { showResult })

    val canSubmit: Boolean
        get() = this is Ready || this is Failed || this is Recovered && !recovery.successful

    fun dismissResult(): BittensorSubmissionState = when (this) {
        is Completed -> Completed()
        is Failed -> Failed()
        is Unresolved -> Unresolved(hash)
        is Recovered -> Recovered(recovery, showResult = false)
        else -> this
    }
}
