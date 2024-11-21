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
import io.novafoundation.nova.common.utils.makeInvisible
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.bottomSheet.description.observeDescription
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.feature_swap_impl.presentation.execution.model.SwapProgressModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionActionButton
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionAssets
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionContainer
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionDetails
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionFinishedStatus
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionNetworkFee
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionPriceDifference
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionProgressViews
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionRate
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionRemainingTime
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionRoute
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionSlippage
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

        swapExecutionToolbar.setHomeButtonListener { viewModel.onBackPressed() }

        swapExecutionRate.setOnClickListener { viewModel.rateClicked() }
        swapExecutionPriceDifference.setOnClickListener { viewModel.priceDifferenceClicked() }
        swapExecutionSlippage.setOnClickListener { viewModel.slippageClicked() }
        swapExecutionNetworkFee.setOnClickListener { viewModel.networkFeeClicked() }
        swapExecutionRoute.setOnClickListener { viewModel.routeClicked() }

        swapExecutionDetails.collapseImmediate()

        onBackPressed { viewModel.onBackPressed() }
    }

    override fun inject() {
        FeatureUtils.getFeature<SwapFeatureComponent>(requireContext(), SwapFeatureApi::class.java)
            .swapExecution()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SwapExecutionViewModel) {
        observeDescription(viewModel)

        viewModel.backAvailableFlow.observe(swapExecutionToolbar::setHomeButtonVisibility)

        viewModel.swapProgressModel.observe(::setSwapProgress)

        viewModel.feeMixin.setupFeeLoading(swapExecutionNetworkFee)

        viewModel.confirmationDetailsFlow.observe {
            swapExecutionAssets.setModel(it.assets)
            swapExecutionRate.showValue(it.rate)
            swapExecutionPriceDifference.showValueOrHide(it.priceDifference)
            swapExecutionSlippage.showValue(it.slippage)
            swapExecutionRoute.setSwapRouteModel(it.swapRouteModel)
        }
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
        swapExecutionProgressViews.makeInvisible()

        swapExecutionFinishedStatus.setImageResource(R.drawable.ic_checkmark_circle_16)
        swapExecutionFinishedStatus.setImageTintRes(R.color.icon_positive)

        swapExecutionTitle.setText(R.string.common_completed)
        swapExecutionTitle.setTextColorRes(R.color.text_positive)
        swapExecutionSubtitle.text = model.at
        swapExecutionSubtitle.setTextColorRes(R.color.text_secondary)

        swapExecutionStepLabel.text = model.operationsLabel
        swapExecutionStepLabel.setTextColorRes(R.color.text_secondary)

        swapExecutionStepShimmer.hideShimmer()
        swapExecutionStepContainer.background = requireContext().getBlockDrawable()

        swapExecutionActionButton.setText(R.string.common_done)
        swapExecutionActionButton.setOnClickListener { viewModel.doneClicked() }
    }

    private fun setSwapFailed(model: SwapProgressModel.Failed) {
        swapExecutionFinishedStatus.makeVisible()
        swapExecutionProgressViews.makeInvisible()

        swapExecutionFinishedStatus.setImageResource(R.drawable.ic_close_circle)
        swapExecutionFinishedStatus.setImageTintRes(R.color.icon_negative)

        swapExecutionTitle.setText(R.string.common_failed)
        swapExecutionTitle.setTextColorRes(R.color.text_negative)
        swapExecutionSubtitle.text = model.at
        swapExecutionSubtitle.setTextColorRes(R.color.text_secondary)

        swapExecutionStepLabel.text = model.reason
        swapExecutionStepLabel.setTextColorRes(R.color.text_primary)

        swapExecutionStepShimmer.hideShimmer()
        swapExecutionStepContainer.background = requireContext().getRoundedCornerDrawable(R.color.error_block_background)

        swapExecutionActionButton.makeVisible()
        swapExecutionActionButton.setText(R.string.common_retry)
        swapExecutionActionButton.setOnClickListener { viewModel.retryClicked() }
    }

    private fun setSwapInProgress(model: SwapProgressModel.InProgress) {
        swapExecutionFinishedStatus.makeInvisible()
        swapExecutionProgressViews.makeVisible()

        swapExecutionRemainingTime.startTimer(model.remainingTime, durationFormatter = UnlabeledSecondsFormatter())

        swapExecutionTitle.setText(R.string.common_do_not_close_app)
        swapExecutionTitle.setTextColorRes(R.color.text_primary)
        swapExecutionSubtitle.text = model.stepDescription
        swapExecutionSubtitle.setTextColorRes(R.color.button_text_accent)

        swapExecutionStepLabel.text = model.operationsLabel
        swapExecutionStepLabel.setTextColorRes(R.color.text_secondary)

        swapExecutionStepShimmer.showShimmer(true)
        swapExecutionStepContainer.background = requireContext().getBlockDrawable()

        swapExecutionActionButton.makeGone()
    }

    private class UnlabeledSecondsFormatter : DurationFormatter {

        override fun format(duration: Duration): String {
            return duration.inWholeSeconds.toString()
        }
    }
}
