package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import androidx.core.view.isVisible
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.ExecutionTimerView
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ViewBittensorSubnetExecutionBinding
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi

fun ViewBittensorSubnetExecutionBinding.showFailure(failure: BittensorStakeResultUi.Failure?) {
    val context = root.context
    val failed = failure != null
    failureIcon.isVisible = failed
    timer.isVisible = !failed
    toolbar.setHomeButtonVisibility(failed)
    title.setTextColorRes(if (failed) R.color.text_negative else R.color.text_primary)
    subtitle.setTextColorRes(if (failed) R.color.text_secondary else R.color.button_text_accent)
    stepContainer.background = if (failed) {
        context.getRoundedCornerDrawable(R.color.error_block_background)
    } else {
        context.getBlockDrawable()
    }
    operationIcon.setImageResource(if (failed) R.drawable.ic_x_clear_filled else R.drawable.ic_bittensor_execution_route)
    operationLabel.setTextColorRes(if (failed) R.color.text_primary else R.color.text_secondary)

    if (failure == null) return

    timer.setState(ExecutionTimerView.State.Error)
    failureIcon.background = context.getRoundedCornerDrawable(R.color.error_block_background, cornerSizeInDp = 32)
    root.setPadding(0, 0, 0, 84.dp(context))
    title.text = if (failure.canRetry) context.getString(R.string.common_failed) else failure.title
    operationLabel.text = if (failure.canRetry) failure.error else failure.description + "\n" + failure.error
}
