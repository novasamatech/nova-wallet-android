package io.novafoundation.nova.feature_swap_impl.presentation.options

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.validation.observeErrors
import io.novafoundation.nova.common.view.bottomSheet.description.observeDescription
import io.novafoundation.nova.common.view.setMessageOrHide
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_api.presentation.model.SlippageSettingsPayload
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.databinding.FragmentSwapSettingsSheetBinding
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent

class SwapSettingsBottomSheetFragment : BaseBottomSheetFragment<SwapOptionsViewModel, FragmentSwapSettingsSheetBinding>() {

    companion object {
        private const val PAYLOAD = "slippage"
        fun getBundle(payload: SlippageSettingsPayload) = Bundle().apply { putParcelable(PAYLOAD, payload) }
    }

    private val presetViews = mutableListOf<TextView>()
    private var selectedTip = -1
    private var custom = false

    override fun createBinding() = FragmentSwapSettingsSheetBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<SwapFeatureComponent>(requireContext(), SwapFeatureApi::class.java)
            .swapOptions().create(this, argument(PAYLOAD)).inject(this)
    }

    override fun initViews() {
        binder.swapSettingsToolbar.setRightActionClickListener { viewModel.backClicked() }
        binder.swapSettingsReset.setOnClickListener { viewModel.resetClicked() }
        binder.swapSettingsApply.setOnClickListener { viewModel.applyClicked() }
        binder.swapSettingsSlippageTitle.setOnClickListener { viewModel.slippageInfoClicked() }
        binder.swapSettingsCustomInput.content.filters += SlippagePrecisionInputFilter()
    }

    override fun subscribe(viewModel: SwapOptionsViewModel) {
        observeDescription(viewModel)
        binder.swapSettingsCustomInput.content.bindTo(viewModel.slippageInput, viewModel.viewModelScope, moveSelectionToEndOnInsertion = true)
        binder.swapSettingsCustomInput.observeErrors(viewModel.slippageInputValidationResult, viewModel.viewModelScope)
        viewModel.defaultSlippage.observe { binder.swapSettingsCustomInput.setHint(it) }
        viewModel.slippageTips.observe { tips ->
            binder.swapSettingsPresets.removeAllViews()
            presetViews.clear()
            (tips + getString(R.string.swap_slippage_custom)).forEachIndexed { index, label ->
                val button = TextView(requireContext()).apply {
                    text = label
                    gravity = Gravity.CENTER
                    setTextAppearance(R.style.TextAppearance_NovaFoundation_SemiBold_Footnote)
                    setTextColor(requireContext().getColor(R.color.text_primary))
                    layoutParams = LinearLayout.LayoutParams(0, 48.dp, 1f).apply { if (index > 0) marginStart = 8.dp }
                    setOnClickListener { if (index == tips.size) viewModel.customClicked() else viewModel.tipClicked(index) }
                }
                presetViews.add(button)
                binder.swapSettingsPresets.addView(button)
            }
            updatePresetSelection()
        }
        viewModel.selectedTip.observe { selectedTip = it; updatePresetSelection() }
        viewModel.showCustomInput.observe {
            custom = it
            binder.swapSettingsCustomInput.setVisible(it)
            updatePresetSelection()
        }
        viewModel.slippageWarningState.observe(binder.swapSettingsAlert::setMessageOrHide)
        viewModel.buttonState.observe { binder.swapSettingsApply.setState(it) }
        viewModel.resetButtonEnabled.observe { binder.swapSettingsReset.isEnabled = it }
    }

    private fun updatePresetSelection() {
        presetViews.forEachIndexed { index, button ->
            val selected = if (custom) index == presetViews.lastIndex else index == selectedTip
            button.background = requireContext().getRoundedCornerDrawable(
                if (selected) R.color.button_background_primary else R.color.chips_background
            )
            button.isSelected = selected
        }
    }
}
