package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.ExecutionTimerView
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ViewBittensorSubnetExecutionBinding

/**
 * Done state of the execution page as on iOS: green "Completed", the date in secondary text and the finished operation count.
 * Bind after [showFailure], which resets the colours for the running and failed states.
 */
fun ViewBittensorSubnetExecutionBinding.showCompleted(operation: CharSequence) {
    timer.setState(ExecutionTimerView.State.Success)
    title.setText(R.string.common_completed)
    title.setTextColorRes(R.color.text_positive)
    subtitle.setTextColorRes(R.color.text_secondary)
    operationLabel.text = operation
}
