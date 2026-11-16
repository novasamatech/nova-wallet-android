package io.novafoundation.nova.feature_swap_impl.presentation.main

import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartSelectionListener
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.inSpans
import android.text.style.TextAppearanceSpan
import kotlinx.coroutines.flow.combine
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.presentation.main.chart.SwapChartHeaderModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartModel
import io.novafoundation.nova.common.utils.keyboard.isKeyboardVisible
import io.novafoundation.nova.common.utils.keyboard.setKeyboardVisibilityListener
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartsView
import android.transition.ChangeBounds
import android.transition.ChangeTransform
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import io.novafoundation.nova.common.utils.insets.applyStatusBarInsets
import io.novafoundation.nova.common.utils.insets.applySystemBarInsets
import io.novafoundation.nova.common.utils.insets.ImeInsetsState
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.hideKeyboard
import io.novafoundation.nova.common.utils.postToUiThread
import io.novafoundation.nova.common.utils.setSelectionEnd
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.bottomSheet.description.observeDescription
import io.novafoundation.nova.common.view.ExpandableViewState
import io.novafoundation.nova.common.view.setMessageOrHide
import io.novafoundation.nova.common.view.setProgressState
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.showLoadingValue
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.setupSwapAmountInput
import io.novafoundation.nova.feature_swap_impl.databinding.FragmentMainSwapSettingsBinding
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.bindGetAsset

abstract class BaseSwapFormFragment : BaseFragment<SwapMainSettingsViewModel, FragmentMainSwapSettingsBinding>() {

    protected abstract val display: SwapFormDisplay

    protected open fun initialPayload(): SwapSettingsPayload = SwapSettingsPayload.Restore

    override fun onResume() {
        super.onResume()
        viewModel.setActive(true)
        showInitialKeyboard()
    }

    private fun showInitialKeyboard() {
        binder.swapMainSettingsPayInput.post {
            viewLifecycleOwner.lifecycleScope.launch {
                val input = when (viewModel.initialKeyboardDirection()) {
                    SwapDirection.SPECIFIED_IN -> binder.swapMainSettingsPayInput.amountInput
                    SwapDirection.SPECIFIED_OUT -> binder.swapMainSettingsReceiveInput.amountInput
                }
                if (input.isShown && viewModel.consumeInitialKeyboard()) {
                    input.requestFocus()
                    input.showSoftKeyboard()
                }
            }
        }
    }

    override fun onPause() {
        viewModel.setActive(false)
        super.onPause()
    }

    override fun applyInsets(rootView: View) {
        if (display == SwapFormDisplay.TAB) {
            rootView.applyStatusBarInsets()
        } else {
            rootView.applySystemBarInsets(imeInsets = ImeInsetsState.ENABLE_IF_SUPPORTED)
        }
    }

    override fun createBinding() = FragmentMainSwapSettingsBinding.inflate(layoutInflater)

    override fun initViews() {
        setupChart()
        binder.swapMainSettingsContinue.prepareForProgress(this)
        setupHeader()
        binder.swapMainSettingsRetry.setOnClickListener { viewModel.retryQuote() }
        binder.swapMainSettingsSlippage.setOnClickListener { viewModel.openOptions() }
        if (viewModel.detailsExpanded.value) binder.swapMainSettingsDetails.expandImmediate()
        binder.swapMainSettingsDetails.setCallback { state ->
            animateDetailsTransition()
            viewModel.setDetailsExpanded(state == ExpandableViewState.EXPANDED)
        }
        binder.swapMainSettingsToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.swapMainSettingsPayInput.setSelectTokenClickListener { viewModel.selectPayToken() }
        binder.swapMainSettingsReceiveInput.setSelectTokenClickListener { viewModel.selectReceiveToken() }
        binder.swapMainSettingsFlip.setOnClickListener {
            viewModel.flipAssets()
        }
        binder.swapMainSettingsDetailsRate.setOnClickListener { viewModel.rateDetailsClicked() }
        binder.swapMainSettingsDetailsNetworkFee.setOnClickListener { viewModel.networkFeeClicked() }
        binder.swapMainSettingsContinue.setOnClickListener { viewModel.continueButtonClicked() }
        binder.swapMainSettingsRoute.setOnClickListener {
            viewModel.routeClicked()

            hideKeyboard()
        }

        binder.swapNovaFeeDisclaimer.text = viewModel.novaFeeDisclaimerText
    }

    private fun setupHeader() {
        val isTab = display == SwapFormDisplay.TAB
        binder.swapMainSettingsToolbar.setVisible(!isTab)
        binder.swapTabHeader.setVisible(isTab)
        binder.swapMainSettingsToolbar.setRightActionClickListener { openSettings() }
        binder.swapTabSettings.setOnClickListener { openSettings() }
    }

    private fun openSettings() {
        hideKeyboard()
        viewModel.openOptions()
    }

    private fun setupChart() {
        binder.swapChart.swapChartPlot.apply {
            setPresentation(PriceChartsView.Presentation.PLOT_ONLY)
            setLoadingStyle(PriceChartsView.LoadingStyle.BLOCK)
            setPeriodSpacing(6)
            setPlotHeight(116)
            setAxisVisible(true)
            onPeriodSelected = viewModel.chart::selectPeriod
            selectionListener = PriceChartSelectionListener(viewModel.chart::selectPoint)
        }
        binder.swapChart.swapChartSparkline.apply {
            setPresentation(PriceChartsView.Presentation.SPARKLINE)
            setPlotHeight(20)
        }
        binder.swapChart.swapChartHeader.setOnClickListener { viewModel.chart.toggleExpanded() }
        binder.swapChart.swapChartRetry.setOnClickListener { viewModel.chart.retry() }
        viewModel.chart.setKeyboardVisible(binder.root.isKeyboardVisible())
        viewLifecycleOwner.lifecycle.setKeyboardVisibilityListener(binder.swapMainSettingsScroll) {
            viewModel.chart.setKeyboardVisible(it)
        }
    }

    private fun observeChart(viewModel: SwapMainSettingsViewModel) {
        viewModel.chart.enabled.observe { binder.swapChart.root.setVisible(it) }
        viewModel.chart.expanded.observe { expanded ->
            val state = if (expanded) ExpandableViewState.EXPANDED else ExpandableViewState.COLLAPSED
            if (binder.swapChart.root.currentState() != state) {
                animateDetailsTransition()
                binder.swapChart.root.setState(state)
            }
            binder.swapChart.root.setBackgroundResource(if (expanded) R.drawable.bg_block_16 else R.drawable.bg_block_12)
            binder.swapChart.swapChartSparkline.setVisible(!expanded)
        }
        viewModel.chart.plot.observe { plot ->
            binder.swapChart.swapChartPlot.setPlotHeight(if (plot.selectedChart is PriceChartModel.Loading) 64 else 116)
            binder.swapChart.swapChartPlot.setSelectedPeriod(plot.selectedPeriodIndex)
            binder.swapChart.swapChartPlot.setAxisLabelFormatter(viewModel.chart.axisFormatter(plot))
            binder.swapChart.swapChartPlot.setCharts(plot.charts)
            binder.swapChart.swapChartSparkline.setSelectedPeriod(plot.selectedPeriodIndex)
            binder.swapChart.swapChartSparkline.setCharts(plot.charts)
            binder.swapChart.swapChartRetry.setVisible(plot.failed)
        }
        combine(
            viewModel.chart.header,
            viewModel.chart.expanded,
            ::Pair
        ).observe { (header, expanded) -> bindChartHeader(header, expanded) }
    }

    private fun bindChartHeader(header: SwapChartHeaderModel, expanded: Boolean) {
        val priceStyle = if (expanded) {
            R.style.TextAppearance_NovaFoundation_SemiBold_SubHeadline_Primary
        } else {
            R.style.TextAppearance_NovaFoundation_Regular_Footnote_Secondary
        }
        val changeStyle = if (expanded) {
            R.style.TextAppearance_NovaFoundation_Regular_Footnote
        } else {
            R.style.TextAppearance_NovaFoundation_SemiBold_Footnote
        }
        binder.swapChart.swapChartTitle.text = buildSpannedString {
            append(header.ticker)
            if (header.price.isNotEmpty()) {
                append("  ")
                inSpans(TextAppearanceSpan(requireContext(), priceStyle)) { append(header.price) }
            }
            if (header.change.isNotEmpty()) {
                append("  ")
                color(requireContext().getColor(header.changeColor)) {
                    inSpans(TextAppearanceSpan(requireContext(), changeStyle)) { append(header.change) }
                }
            }
        }
        binder.swapChart.swapChartDate.setTextOrHide(header.date)
    }

    private fun animateDetailsTransition() {
        val transition = TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            duration = 300L
            addTransition(ChangeBounds())
            addTransition(Fade())
            addTransition(ChangeTransform())
        }

        TransitionManager.beginDelayedTransition(binder.swapMainSettingsScroll, transition)
    }

    override fun inject() {
        FeatureUtils.getFeature<SwapFeatureComponent>(
            requireContext(),
            SwapFeatureApi::class.java
        )
            .swapMainSettings()
            .create(this, if (display == SwapFormDisplay.TAB) requireParentFragment() else this, initialPayload(), display)
            .inject(this)
    }

    override fun subscribe(viewModel: SwapMainSettingsViewModel) {
        observeChart(viewModel)
        observeDescription(viewModel)
        observeValidations(viewModel)
        setupSwapAmountInput(viewModel.amountInInput, binder.swapMainSettingsPayInput, binder.swapMainSettingsMaxAmount)
        setupSwapAmountInput(viewModel.amountOutInput, binder.swapMainSettingsReceiveInput, maxAvailableView = null)
        viewModel.getAssetOptionsMixin.bindGetAsset(binder.swapMainSettingsGetAssetIn)

        viewModel.feeMixin.setupFeeLoading(binder.swapMainSettingsDetailsNetworkFee)

        viewModel.amountInInput.assetModel.observe { if (it.showInput) showInitialKeyboard() }
        viewModel.amountOutInput.assetModel.observe { if (it.showInput) showInitialKeyboard() }
        viewModel.receiveBalance.observe { binder.swapMainSettingsReceiveBalance.text = it }
        viewModel.quoteProblem.observe(binder.swapMainSettingsAlert::setMessageOrHide)
        viewModel.quoteFailed.observe { binder.swapMainSettingsRetry.setVisible(it) }
        viewModel.priceImpactWarning.observe(binder.swapMainSettingsPriceImpactAlert::setMessageOrHide)
        viewModel.priceImpactDetails.observe { binder.swapMainSettingsPriceImpact.showValue(it ?: "—") }
        viewModel.slippageDetails.observe(binder.swapMainSettingsSlippage::showValue)
        viewModel.minimumReceived.observe { binder.swapMainSettingsMinReceived.showValue(it ?: "—") }

        viewModel.rateDetails.observe { binder.swapMainSettingsDetailsRate.showLoadingValue(it) }
        viewModel.swapRouteState.observe(binder.swapMainSettingsRoute::setSwapRouteState)
        viewModel.swapExecutionTime.observe(binder.swapMainSettingsExecutionTime::showLoadingValue)
        viewModel.showDetails.observe { binder.swapMainSettingsDetails.setVisible(it) }
        viewModel.buttonState.observe(binder.swapMainSettingsContinue::setState)

        viewModel.swapDirectionFlipped.observeEvent {
            postToUiThread {
                val field = when (it) {
                    SwapDirection.SPECIFIED_IN -> binder.swapMainSettingsPayInput
                    SwapDirection.SPECIFIED_OUT -> binder.swapMainSettingsReceiveInput
                }

                field.requestFocus()
                field.amountInput.setSelectionEnd()
            }
        }

        viewModel.validationProgress.observe(binder.swapMainSettingsContinue::setProgressState)

        viewModel.showNovaFeeDisclaimer.observe { binder.swapNovaFeeDisclaimer.setVisible(it) }
    }
}
