package io.novafoundation.nova.feature_swap_impl.presentation.options

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.validation.observeErrors
import io.novafoundation.nova.common.view.bottomSheet.description.observeDescription
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.setMessageOrHide
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent

class SwapOptionsFragment : BaseFragment<SwapOptionsViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_swap_options, container, false)
    }

    override fun initViews() {
        swapOptionsToolbar.applyStatusBarInsets()
        swapOptionsToolbar.setHomeButtonListener { viewModel.backClicked() }
        swapOptionsToolbar.setRightActionClickListener { viewModel.resetClicked() }
        swapOptionsApplyButton.setOnClickListener { viewModel.applyClicked() }
        swapOptionsSlippageTitle.setOnClickListener { viewModel.slippageInfoClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<SwapFeatureComponent>(
            requireContext(),
            SwapFeatureApi::class.java
        )
            .swapOptions()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SwapOptionsViewModel) {
        observeDescription(viewModel)
        swapOptionsSlippageInput.content.bindTo(viewModel.slippageInput, viewModel.viewModelScope, moveSelectionToEndOnInsertion = true)
        viewModel.defaultSlippage.observe { swapOptionsSlippageInput.setHint(it) }
        viewModel.slippageTips.observe {
            swapOptionsSlippageInput.clearTips()
            it.forEachIndexed { index, text ->
                swapOptionsSlippageInput.addTextTip(text, R.color.text_primary) { viewModel.tipClicked(index) }
            }
        }
        viewModel.buttonState.observe { swapOptionsApplyButton.setState(it) }
        swapOptionsSlippageInput.observeErrors(viewModel.slippageInputValidationResult, viewModel.viewModelScope)
        viewModel.slippageWarningState.observe { swapOptionsAlert.setMessageOrHide(it) }
        viewModel.resetButtonEnabled.observe { swapOptionsToolbar.setRightActionEnabled(it) }
    }
}
