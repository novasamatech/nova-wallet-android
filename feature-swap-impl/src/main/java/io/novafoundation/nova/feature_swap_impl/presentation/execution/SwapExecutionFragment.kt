package io.novafoundation.nova.feature_swap_impl.presentation.execution

import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextSwitcher
import android.widget.TextView
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setCurrentText
import io.novafoundation.nova.common.utils.setText
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.bottomSheet.description.observeDescription
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.feature_swap_impl.presentation.execution.model.SwapProgressModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionActionButton
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionAssets
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionContainer
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionDetails
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionNetworkFee
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionPriceDifference
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionRate
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionRoute
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionSlippage
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionStepContainer
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionStepLabel
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionStepShimmer
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionSubtitleSwitcher
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionTimer
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionTitleSwitcher
import kotlinx.android.synthetic.main.fragment_swap_execution.swapExecutionToolbar

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

        swapExecutionTitleSwitcher.applyTitleFactory()
        swapExecutionSubtitleSwitcher.applySubtitleFactory()

        swapExecutionTitleSwitcher.applyAnimators()
        swapExecutionSubtitleSwitcher.applyAnimators()
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
        swapExecutionTimer.setState(ExecutionTimerView.State.Success)

        swapExecutionTitleSwitcher.setText(getString(R.string.common_completed), colorRes = R.color.text_positive)
        swapExecutionSubtitleSwitcher.setText(model.at, colorRes = R.color.text_secondary)

        swapExecutionStepLabel.text = model.operationsLabel
        swapExecutionStepLabel.setTextColorRes(R.color.text_secondary)

        swapExecutionStepShimmer.hideShimmer()
        swapExecutionStepContainer.background = requireContext().getBlockDrawable()

        swapExecutionActionButton.setText(R.string.common_done)
        swapExecutionActionButton.setOnClickListener { viewModel.doneClicked() }
    }

    private fun setSwapFailed(model: SwapProgressModel.Failed) {
        swapExecutionTimer.setState(ExecutionTimerView.State.Error)

        swapExecutionTitleSwitcher.setText(getString(R.string.common_failed), colorRes = R.color.text_negative)
        swapExecutionSubtitleSwitcher.setText(model.at, colorRes = R.color.text_secondary)

        swapExecutionStepLabel.text = model.reason
        swapExecutionStepLabel.setTextColorRes(R.color.text_primary)

        swapExecutionStepShimmer.hideShimmer()
        swapExecutionStepContainer.background = requireContext().getRoundedCornerDrawable(R.color.error_block_background)

        swapExecutionActionButton.makeVisible()
        swapExecutionActionButton.setText(R.string.common_retry)
        swapExecutionActionButton.setOnClickListener { viewModel.retryClicked() }
    }

    private fun setSwapInProgress(model: SwapProgressModel.InProgress) {
        swapExecutionTimer.setState(ExecutionTimerView.State.CountdownTimer(model.remainingTime))

        swapExecutionTitleSwitcher.setCurrentText(getString(R.string.common_do_not_close_app), colorRes = R.color.text_primary)
        swapExecutionSubtitleSwitcher.setCurrentText(model.stepDescription, colorRes = R.color.button_text_accent)

        swapExecutionStepLabel.text = model.operationsLabel
        swapExecutionStepLabel.setTextColorRes(R.color.text_secondary)

        swapExecutionStepShimmer.showShimmer(true)
        swapExecutionStepContainer.background = requireContext().getBlockDrawable()

        swapExecutionActionButton.makeGone()
    }

    private fun TextSwitcher.applyTitleFactory() {
        setFactory {
            val textView = TextView(context, null, 0, R.style.TextAppearance_NovaFoundation_Bold_Title1)
            textView.setGravity(Gravity.CENTER)
            textView
        }
    }

    private fun TextSwitcher.applySubtitleFactory() {
        setFactory {
            val textView = TextView(context, null, 0, R.style.TextAppearance_NovaFoundation_SemiBold_Body)
            textView.setGravity(Gravity.CENTER)
            textView.setSingleLine()
            textView.ellipsize = TextUtils.TruncateAt.END
            textView
        }
    }

    private fun TextSwitcher.applyAnimators() {
        inAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_scale_in)
        outAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_scale_out)
    }
}
