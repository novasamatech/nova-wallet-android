package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import android.animation.ObjectAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.updateLayoutParams
import io.novafoundation.nova.common.utils.dp
import android.view.animation.LinearInterpolator
import androidx.core.view.isVisible
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.BottomSheetBittensorStakeResultBinding
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorResultAction
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi

private const val SPINNER_ANIMATION_DURATION = 1_000L
private const val BUTTON_GAP_DP = 6

class BittensorStakeResultBottomSheet(
    context: Context,
    onAction: (BittensorResultAction) -> Unit
) : BaseBottomSheet<BottomSheetBittensorStakeResultBinding>(context) {

    override val binder = BottomSheetBittensorStakeResultBinding.inflate(LayoutInflater.from(context))

    private var actions: List<BittensorResultAction> = emptyList()

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
        // The sheet only closes through its buttons, as the iOS result sheet cannot be hidden.
        setCancelable(false)
        setCanceledOnTouchOutside(false)

        binder.bittensorStakeResultSecondaryAction.setOnClickListener { actions.getOrNull(actions.size - 2)?.let(onAction) }
        binder.bittensorStakeResultPrimaryAction.setOnClickListener { actions.lastOrNull()?.let(onAction) }
    }

    fun setState(state: BittensorStakeResultUi) {
        val inProgress = state is BittensorStakeResultUi.Progress
        val success = state as? BittensorStakeResultUi.Success
        val failure = state as? BittensorStakeResultUi.Failure

        binder.bittensorStakeResultProgress.setVisible(inProgress)
        binder.bittensorStakeResultSuccess.setVisible(success != null)
        binder.bittensorStakeResultFailure.setVisible(failure != null || state is BittensorStakeResultUi.Unconfirmed)
        binder.bittensorStakeResultError.isVisible = failure != null
        binder.bittensorStakeResultFailureFees.isVisible = failure?.fee != null
        binder.bittensorStakeResultAmount.isVisible = success?.amount != null
        binder.bittensorStakeResultValidator.isVisible = success?.validator != null
        binder.bittensorStakeResultStakeAfter.isVisible = success?.stakeAfter != null

        if (inProgress) {
            spinnerAnimator.start()
        } else {
            spinnerAnimator.cancel()
        }

        when (state) {
            is BittensorStakeResultUi.Progress -> showProgress(state)
            is BittensorStakeResultUi.Success -> showSuccess(state)
            is BittensorStakeResultUi.Failure -> showFailure(state)
            is BittensorStakeResultUi.Unconfirmed -> showUnconfirmed(state)
        }

        showActions(state.actions)
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
        state.amount?.let { binder.bittensorStakeResultAmount.showValue(it.token, it.fiat) }
        state.validator?.let { binder.bittensorStakeResultValidator.showValue(it) }
        state.stakeAfter?.let { binder.bittensorStakeResultStakeAfter.showValue(it) }
        binder.bittensorStakeResultFee.showValue(state.fee.amount, state.fee.fiatAmount)
    }

    private fun showFailure(state: BittensorStakeResultUi.Failure) {
        binder.bittensorStakeResultStatusIcon.setImageResource(R.drawable.ic_bittensor_staking_error)
        binder.bittensorStakeResultFailureTitle.text = state.title
        binder.bittensorStakeResultFailureDescription.text = state.description
        state.fee?.let { binder.bittensorStakeResultFailureFee.showValue(it.amount, it.fiatAmount) }
        binder.bittensorStakeResultError.setMessage(state.error)
    }

    private fun showUnconfirmed(state: BittensorStakeResultUi.Unconfirmed) {
        binder.bittensorStakeResultStatusIcon.setImageResource(R.drawable.ic_time_24)
        binder.bittensorStakeResultFailureTitle.text = state.title
        binder.bittensorStakeResultFailureDescription.text = listOfNotNull(
            state.description,
            state.transactionHash?.let { context.getString(R.string.bittensor_result_transaction_hash, it) }
        ).joinToString("\n\n")
    }

    /** One button is the primary one; with two, the first is secondary (iOS `bindButtons`). */
    private fun showActions(actions: List<BittensorResultAction>) {
        this.actions = actions
        val secondary = actions.getOrNull(actions.size - 2)
        binder.bittensorStakeResultActions.isVisible = actions.isNotEmpty()
        binder.bittensorStakeResultSecondaryAction.isVisible = secondary != null
        secondary?.let { binder.bittensorStakeResultSecondaryAction.setText(it.title) }
        actions.lastOrNull()?.let { binder.bittensorStakeResultPrimaryAction.setText(it.title) }
        binder.bittensorStakeResultPrimaryAction.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            marginStart = if (secondary != null) BUTTON_GAP_DP.dp(context) else 0
        }
    }
}

@get:StringRes
private val BittensorResultAction.title: Int
    get() = when (this) {
        BittensorResultAction.DONE -> R.string.common_done
        BittensorResultAction.TRY_AGAIN -> R.string.common_try_again
        BittensorResultAction.CLOSE -> R.string.common_close
        BittensorResultAction.VIEW_POSITION -> R.string.bittensor_view_position
        BittensorResultAction.DISCOVER_SUBNETS -> R.string.bittensor_discover_subnets
        BittensorResultAction.YOUR_BITTENSOR -> R.string.bittensor_your_positions
        BittensorResultAction.BACK_TO_POSITION -> R.string.bittensor_back_to_position
    }
