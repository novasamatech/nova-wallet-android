package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ViewBittensorSubnetExecutionBinding
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi

/** Bind after the normal execution fields. Unconfirmed is terminal for this watch, not a failed trade. */
fun ViewBittensorSubnetExecutionBinding.showUnconfirmed(state: BittensorStakeResultUi.Unconfirmed) {
    val context = root.context
    timer.stopTimer()
    timer.isVisible = false
    failureIcon.isVisible = true
    failureIcon.setImageResource(R.drawable.ic_time_24)
    failureIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_secondary))
    failureIcon.background = context.getBlockDrawable()
    toolbar.setHomeButtonVisibility(true)
    title.setTextColorRes(R.color.text_primary)
    title.text = state.title
    subtitle.setTextColorRes(R.color.text_secondary)
    stepContainer.background = context.getBlockDrawable()
    operationIcon.setImageResource(R.drawable.ic_time_24)
    operationLabel.setTextColorRes(R.color.text_secondary)
    operationLabel.text = listOfNotNull(
        state.description,
        state.transactionHash?.let { context.getString(R.string.bittensor_result_transaction_hash, it) }
    ).joinToString("\n\n")
}
