package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import android.animation.ObjectAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.view.isVisible
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_staking_impl.databinding.BottomSheetBittensorStakeResultBinding
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi

private const val SPINNER_ANIMATION_DURATION = 1_000L

class BittensorStakeResultBottomSheet(
    context: Context,
    onClose: () -> Unit,
    onTryAgain: () -> Unit,
    onDiscoverSubnets: () -> Unit,
    onViewPosition: () -> Unit
) : BaseBottomSheet<BottomSheetBittensorStakeResultBinding>(context) {

    override val binder = BottomSheetBittensorStakeResultBinding.inflate(LayoutInflater.from(context))

    private val spinnerAnimator = ObjectAnimator.ofFloat(
        binder.bittensorStakeResultSpinner,
        View.ROTATION,
        0f,
        360f
    ).apply {
        duration = SPINNER_ANIMATION_DURATION
        interpolator = LinearInterpolator()
        repeatCount = ObjectAnimator.INFINITE
    }

    init {
        binder.bittensorStakeResultClose.setOnClickListener { onClose() }
        binder.bittensorStakeResultTryAgain.setOnClickListener { onTryAgain() }
        binder.bittensorStakeResultDiscoverSubnets.setOnClickListener { onDiscoverSubnets() }
        binder.bittensorStakeResultViewPosition.setOnClickListener { onViewPosition() }
    }

    fun setState(state: BittensorStakeResultUi) {
        val inProgress = state is BittensorStakeResultUi.Progress

        binder.bittensorStakeResultProgress.setVisible(inProgress)
        binder.bittensorStakeResultSuccess.setVisible(state is BittensorStakeResultUi.Success || state is BittensorStakeResultUi.Recovered && state.successful)
        binder.bittensorStakeResultFailure.setVisible(state is BittensorStakeResultUi.Failure || state is BittensorStakeResultUi.Recovered && !state.successful)
        binder.bittensorStakeResultValidator.isVisible = state is BittensorStakeResultUi.Success
        binder.bittensorStakeResultFee.isVisible = state is BittensorStakeResultUi.Success
        (binder.bittensorStakeResultFee.parent as View).isVisible = state is BittensorStakeResultUi.Success

        setCancelable(!inProgress)
        setCanceledOnTouchOutside(!inProgress)

        if (inProgress) {
            spinnerAnimator.start()
        } else {
            spinnerAnimator.cancel()
        }

        when (state) {
            is BittensorStakeResultUi.Progress -> showProgress(state)
            is BittensorStakeResultUi.Success -> showSuccess(state)
            is BittensorStakeResultUi.Failure -> showFailure(state)
            is BittensorStakeResultUi.Recovered -> {
                if (state.successful) {
                    binder.bittensorStakeResultSuccessTitle.text = state.title
                    binder.bittensorStakeResultSuccessDescription.text = state.description
                } else {
                    showFailure(
                        BittensorStakeResultUi.Failure(
                            state.title,
                            state.description,
                            state.transactionHash,
                            canRetry = false
                        )
                    )
                }
            }
        }
    }

    override fun dismiss() {
        spinnerAnimator.cancel()
        super.dismiss()
    }

    private fun showProgress(state: BittensorStakeResultUi.Progress) {
        binder.bittensorStakeResultProgressTitle.text = state.title
        binder.bittensorStakeResultProgressDescription.text = state.description
    }

    private fun showSuccess(state: BittensorStakeResultUi.Success) {
        binder.bittensorStakeResultSuccessTitle.text = state.title
        binder.bittensorStakeResultSuccessDescription.text = state.description
        binder.bittensorStakeResultValidator.showValue(state.validator)
        binder.bittensorStakeResultFee.showValue(state.fee.amount, state.fee.fiatAmount)
    }

    private fun showFailure(state: BittensorStakeResultUi.Failure) {
        binder.bittensorStakeResultTryAgain.isVisible = state.canRetry
        binder.bittensorStakeResultFailureTitle.text = state.title
        binder.bittensorStakeResultFailureDescription.text = state.description
        binder.bittensorStakeResultError.setMessage(state.error)
    }
}
