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
import io.novafoundation.nova.common.utils.input.DecimalInputFilter
import io.novafoundation.nova.common.validation.observeErrors
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.setTextOrHide
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import kotlinx.android.synthetic.main.fragment_swap_options.swapOptionsAlert
import kotlinx.android.synthetic.main.fragment_swap_options.swapOptionsApplyButton
import kotlinx.android.synthetic.main.fragment_swap_options.swapOptionsSlippageInput
import kotlinx.android.synthetic.main.fragment_swap_options.swapOptionsToolbar

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

        swapOptionsSlippageInput.content.filters = arrayOf(DecimalInputFilter())
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
        viewModel.slippageWarningState.observe { swapOptionsAlert.setTextOrHide(it) }
        viewModel.resetButtonEnabled.observe { swapOptionsToolbar.setRightActionEnabled(it) }
    }
}
