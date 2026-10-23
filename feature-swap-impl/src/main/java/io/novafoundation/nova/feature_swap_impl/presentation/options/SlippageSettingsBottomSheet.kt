package io.novafoundation.nova.feature_swap_impl.presentation.options

import android.os.Bundle
import android.view.WindowManager
import androidx.lifecycle.viewModelScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.validation.observeErrors
import io.novafoundation.nova.common.view.setMessageOrHide
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_api.presentation.model.SlippageSettingsPayload
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.databinding.FragmentSlippageSettingsSheetBinding
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent

/**
 * Slippage for flows that pass their own [SlippageSettingsPayload] (Bittensor staking): a content-sized sheet with a single
 * Done action, as on iOS. The full-screen swap settings stay on [SwapOptionsFragment].
 */
class SlippageSettingsBottomSheet : BaseBottomSheetFragment<SwapOptionsViewModel, FragmentSlippageSettingsSheetBinding>() {

    companion object {

        private const val KEY_PAYLOAD = "SlippageSettingsBottomSheet.payload"

        fun getBundle(payload: SlippageSettingsPayload) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, payload)
        }
    }

    override fun createBinding() = FragmentSlippageSettingsSheetBinding.inflate(layoutInflater)

    override fun initViews() {
        // The sheet holds a text field: resize so the Done button stays above the keyboard.
        requireDialog().window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        with(getBehaviour()) {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
        binder.slippageSettingsInput.content.filters += SlippagePrecisionInputFilter()
        binder.slippageSettingsDone.setOnClickListener { viewModel.applyClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<SwapFeatureComponent>(
            requireContext(),
            SwapFeatureApi::class.java
        )
            .swapOptions()
            .create(this, argument<SlippageSettingsPayload>(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: SwapOptionsViewModel) {
        binder.slippageSettingsInput.content.bindTo(viewModel.slippageInput, viewModel.viewModelScope, moveSelectionToEndOnInsertion = true)
        viewModel.slippageTips.observe {
            binder.slippageSettingsInput.clearTips()
            it.forEachIndexed { index, text ->
                binder.slippageSettingsInput.addTextTip(text, R.color.text_primary) { viewModel.tipClicked(index) }
            }
        }
        viewModel.buttonState.observe(binder.slippageSettingsDone::setState)
        binder.slippageSettingsInput.observeErrors(viewModel.slippageInputValidationResult, viewModel.viewModelScope)
        viewModel.slippageWarningState.observe(binder.slippageSettingsAlert::setMessageOrHide)
    }
}
