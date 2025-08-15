package io.novafoundation.nova.feature_swap_impl.presentation.options

import androidx.lifecycle.viewModelScope

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.validation.observeErrors
import io.novafoundation.nova.common.view.bottomSheet.description.observeDescription
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.setMessageOrHide
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.databinding.FragmentSwapOptionsBinding
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent

class SwapOptionsFragment : BaseFragment<SwapOptionsViewModel, FragmentSwapOptionsBinding>() {

    override fun createBinding() = FragmentSwapOptionsBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.swapOptionsToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.swapOptionsToolbar.setRightActionClickListener { viewModel.resetClicked() }
        binder.swapOptionsApplyButton.setOnClickListener { viewModel.applyClicked() }
        binder.swapOptionsSlippageTitle.setOnClickListener { viewModel.slippageInfoClicked() }
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
        binder.swapOptionsSlippageInput.content.bindTo(viewModel.slippageInput, viewModel.viewModelScope, moveSelectionToEndOnInsertion = true)
        viewModel.defaultSlippage.observe { binder.swapOptionsSlippageInput.setHint(it) }
        viewModel.slippageTips.observe {
            binder.swapOptionsSlippageInput.clearTips()
            it.forEachIndexed { index, text ->
                binder.swapOptionsSlippageInput.addTextTip(text, R.color.text_primary) { viewModel.tipClicked(index) }
            }
        }
        viewModel.buttonState.observe { binder.swapOptionsApplyButton.setState(it) }
        binder.swapOptionsSlippageInput.observeErrors(viewModel.slippageInputValidationResult, viewModel.viewModelScope)
        viewModel.slippageWarningState.observe { binder.swapOptionsAlert.setMessageOrHide(it) }
        viewModel.resetButtonEnabled.observe { binder.swapOptionsToolbar.setRightActionEnabled(it) }
    }
}
