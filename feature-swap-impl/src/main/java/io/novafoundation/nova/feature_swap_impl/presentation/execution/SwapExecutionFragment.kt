package io.novafoundation.nova.feature_swap_impl.presentation.execution

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.formatting.duration.DurationFormatter
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setBackgroundTintRes
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.feature_swap_impl.presentation.execution.model.SwapProgressModel
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionContainer
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionFinishedStatus
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionProgressViews
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionRemainingTime
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionStepContainer
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionStepLabel
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionStepShimmer
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionSubtitle
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionTitle
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionToolbar
import kotlin.time.Duration

class SwapExecutionFragment : BaseFragment<SwapExecutionViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_swap_execution, container, false)
    }

    override fun initViews() {
        swapExecutionContainer.applyStatusBarInsets()

        onBackPressed { viewModel.onBackPressed() }
    }

    override fun inject() {
        FeatureUtils.getFeature<SwapFeatureComponent>(requireContext(), SwapFeatureApi::class.java)
            .swapExecution()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SwapExecutionViewModel) {
        viewModel.backAvailableFlow.observe(swapExecutionToolbar::setHomeButtonVisibility)

        viewModel.swapProgressModel.observe(::setSwapProgress)
    }

    private fun setSwapProgress(model: SwapProgressModel) {
        when (model) {
            is SwapProgressModel.Completed -> setSwapCompleted(model)
            is SwapProgressModel.Failed -> setSwapFailed(model)
            is SwapProgressModel.InProgress -> setSwapInProgress(model)
        }
    }

    private fun setSwapCompleted(model: SwapProgressModel.Completed) {
        swapExecutionFinishedStatus.makeVisible()
        swapExecutionProgressViews.makeGone()

        swapExecutionFinishedStatus.setBackgroundTintRes(R.color.icon_positive_background)
        swapExecutionFinishedStatus.setImageResource(R.drawable.ic_checkmark)
        swapExecutionFinishedStatus.setImageTintRes(R.color.icon_positive)

        swapExecutionTitle.setText(R.string.common_completed)
        swapExecutionSubtitle.text = model.at

        swapExecutionStepLabel.text = model.operationsLabel
        swapExecutionStepLabel.setTextColorRes(R.color.text_secondary)
        swapExecutionStepShimmer.stopShimmer()
        swapExecutionStepContainer.background = requireContext().getBlockDrawable()
    }

    private fun setSwapFailed(model: SwapProgressModel.Failed) {
        swapExecutionFinishedStatus.makeVisible()
        swapExecutionProgressViews.makeGone()

        swapExecutionFinishedStatus.backgroundTintList = null
        swapExecutionFinishedStatus.setImageResource(R.drawable.ic_close)
        swapExecutionFinishedStatus.setImageTintRes(R.color.icon_negative)

        swapExecutionTitle.setText(R.string.common_failed)
        swapExecutionSubtitle.text = model.at

        swapExecutionStepLabel.text = model.reason
        swapExecutionStepLabel.setTextColorRes(R.color.text_primary)
        swapExecutionStepShimmer.stopShimmer()
        swapExecutionStepContainer.background = requireContext().getRoundedCornerDrawable(R.color.error_block_background)
    }

    private fun setSwapInProgress(model: SwapProgressModel.InProgress) {
        swapExecutionFinishedStatus.makeGone()
        swapExecutionProgressViews.makeVisible()

        swapExecutionRemainingTime.startTimer(model.remainingTime, durationFormatter = UnlabeledSecondsFormatter())

        swapExecutionTitle.setText(R.string.common_do_not_close_app)
        swapExecutionSubtitle.text = model.stepDescription

        swapExecutionStepLabel.text = model.operationsLabel
        swapExecutionStepLabel.setTextColorRes(R.color.text_secondary)
        swapExecutionStepShimmer.startShimmer()
        swapExecutionStepContainer.background = requireContext().getBlockDrawable()
    }

    private class UnlabeledSecondsFormatter : DurationFormatter {

        override fun format(duration: Duration): String {
            return duration.inWholeSeconds.toString()
        }
    }
}
