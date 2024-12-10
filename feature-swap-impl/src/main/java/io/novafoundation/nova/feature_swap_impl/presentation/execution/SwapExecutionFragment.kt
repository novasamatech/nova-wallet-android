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
import io.novafoundation.nova.feature_swap_impl.databinding.FragmentSwapConfirmationBinding
import io.novafoundation.nova.feature_swap_impl.databinding.FragmentSwapExecutionBinding
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.feature_swap_impl.presentation.execution.model.SwapProgressModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.setupFeeLoading

class SwapExecutionFragment : BaseFragment<SwapExecutionViewModel, FragmentSwapExecutionBinding>() {

    override fun createBinding() = FragmentSwapExecutionBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.swapExecutionContainer.applyStatusBarInsets()

        binder.swapExecutionRate.setOnClickListener { viewModel.rateClicked() }
        binder.swapExecutionPriceDifference.setOnClickListener { viewModel.priceDifferenceClicked() }
        binder.swapExecutionSlippage.setOnClickListener { viewModel.slippageClicked() }
        binder.swapExecutionNetworkFee.setOnClickListener { viewModel.networkFeeClicked() }
        binder.swapExecutionRoute.setOnClickListener { viewModel.routeClicked() }

        binder.swapExecutionDetails.collapseImmediate()

        onBackPressed { /* suppress back presses */ }

        binder.swapExecutionTitleSwitcher.applyTitleFactory()
        binder.swapExecutionSubtitleSwitcher.applySubtitleFactory()

        binder.swapExecutionTitleSwitcher.applyAnimators()
        binder.swapExecutionSubtitleSwitcher.applyAnimators()

        binder.swapExecutionToolbar.setHomeButtonVisibility(false)
    }

    override fun inject() {
        FeatureUtils.getFeature<SwapFeatureComponent>(requireContext(), SwapFeatureApi::class.java)
            .swapExecution()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SwapExecutionViewModel) {
        observeDescription(viewModel)

        viewModel.swapProgressModel.observe(::setSwapProgress)

        viewModel.feeMixin.setupFeeLoading(binder.swapExecutionNetworkFee)

        viewModel.confirmationDetailsFlow.observe {
            binder.swapExecutionAssets.setModel(it.assets)
            binder.swapExecutionRate.showValue(it.rate)
            binder.swapExecutionPriceDifference.showValueOrHide(it.priceDifference)
            binder.swapExecutionSlippage.showValue(it.slippage)
            binder.swapExecutionRoute.setSwapRouteModel(it.swapRouteModel)
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
        binder.swapExecutionTimer.setState(ExecutionTimerView.State.Success)

        binder.swapExecutionTitleSwitcher.setText(getString(R.string.common_completed), colorRes = R.color.text_positive)
        binder.swapExecutionSubtitleSwitcher.setText(model.at, colorRes = R.color.text_secondary)

        binder.swapExecutionStepLabel.text = model.operationsLabel
        binder.swapExecutionStepLabel.setTextColorRes(R.color.text_secondary)

        binder.swapExecutionStepShimmer.hideShimmer()
        binder.swapExecutionStepContainer.background = requireContext().getBlockDrawable()

        binder.swapExecutionActionButton.makeVisible()
        binder.swapExecutionActionButton.setText(R.string.common_done)
        binder.swapExecutionActionButton.setOnClickListener { viewModel.doneClicked() }
    }

    private fun setSwapFailed(model: SwapProgressModel.Failed) {
        binder.swapExecutionTimer.setState(ExecutionTimerView.State.Error)

        binder.swapExecutionTitleSwitcher.setText(getString(R.string.common_failed), colorRes = R.color.text_negative)
        binder.swapExecutionSubtitleSwitcher.setText(model.at, colorRes = R.color.text_secondary)

        binder.swapExecutionStepLabel.text = model.reason
        binder.swapExecutionStepLabel.setTextColorRes(R.color.text_primary)

        binder.swapExecutionStepShimmer.hideShimmer()
        binder.swapExecutionStepContainer.background = requireContext().getRoundedCornerDrawable(R.color.error_block_background)

        binder.swapExecutionActionButton.makeVisible()
        binder.swapExecutionActionButton.setText(R.string.common_try_again)
        binder.swapExecutionActionButton.setOnClickListener { viewModel.retryClicked() }
    }

    private fun setSwapInProgress(model: SwapProgressModel.InProgress) {
        binder.swapExecutionTimer.setState(ExecutionTimerView.State.CountdownTimer(model.remainingTime))

        binder.swapExecutionTitleSwitcher.setCurrentText(getString(R.string.common_do_not_close_app), colorRes = R.color.text_primary)
        binder.swapExecutionSubtitleSwitcher.setCurrentText(model.stepDescription, colorRes = R.color.button_text_accent)

        binder.swapExecutionStepLabel.text = model.operationsLabel
        binder.swapExecutionStepLabel.setTextColorRes(R.color.text_secondary)

        binder.swapExecutionStepShimmer.showShimmer(true)
        binder.swapExecutionStepContainer.background = requireContext().getBlockDrawable()

        binder.swapExecutionActionButton.makeGone()
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
