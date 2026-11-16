package io.novafoundation.nova.feature_swap_impl.presentation.options

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.hideKeyboard
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.validation.observeErrors
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawableWithRipple
import io.novafoundation.nova.common.view.shape.getTopRoundedCornerDrawable
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_api.presentation.model.SlippageSettingsPayload
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.databinding.FragmentSwapSettingsSheetBinding
import io.novafoundation.nova.feature_swap_impl.databinding.ItemSwapSlippagePresetBinding
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent

class SwapSettingsBottomSheetFragment : BaseBottomSheetFragment<SwapSettingsSheetViewModel, FragmentSwapSettingsSheetBinding>() {

    companion object {

        private const val PAYLOAD = "slippage"

        fun getBundle(payload: SlippageSettingsPayload) = Bundle().apply { putParcelable(PAYLOAD, payload) }
    }

    private val presetViews = mutableListOf<ItemSwapSlippagePresetBinding>()

    override fun createBinding() = FragmentSwapSettingsSheetBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<SwapFeatureComponent>(requireContext(), SwapFeatureApi::class.java)
            .swapOptions()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.doOnLayout { sheet ->
            sheet.background = requireContext().getTopRoundedCornerDrawable(R.color.bottom_sheet_background, cornerSizeInDp = 16)
        }
        getBehaviour().state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun initViews() {
        binder.swapSettingsReset.background = requireContext().getRoundedCornerDrawableWithRipple(R.color.swap_settings_secondary_button)
        binder.swapSettingsReset.setOnClickListener {
            finishInput()
            viewModel.resetClicked()
        }
        binder.swapSettingsApply.setOnClickListener {
            finishInput()
            viewModel.applyClicked()
        }
        binder.swapSettingsCustomInput.content.apply {
            filters += SlippagePrecisionInputFilter()
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    finishInput()
                    true
                } else {
                    false
                }
            }
        }
        setupPresets()
    }

    override fun subscribe(viewModel: SwapSettingsSheetViewModel) {
        val scope = viewLifecycleOwner.lifecycleScope
        binder.swapSettingsCustomInput.content.bindTo(
            flow = viewModel.selection,
            scope = scope,
            moveSelectionToEndOnInsertion = true,
            toT = { viewModel.selectionFromInput(it) },
            fromT = { (it as? SwapSlippageSelection.Custom)?.input.orEmpty() }
        )
        binder.swapSettingsCustomInput.observeErrors(viewModel.slippageInputValidationResult, scope)
        viewModel.selectedTip.observe(::updatePresetSelection)
        viewModel.slippageWarningState.observe(binder.swapSettingsWarning::setTextOrHide)
        viewModel.buttonState.observe { binder.swapSettingsApply.setState(it) }
    }

    private fun setupPresets() {
        binder.swapSettingsPresets.removeAllViews()
        presetViews.clear()
        viewModel.slippageTips.forEachIndexed { index, label ->
            val preset = ItemSwapSlippagePresetBinding.inflate(layoutInflater, binder.swapSettingsPresets, false)
            preset.swapSlippagePresetLabel.text = label
            preset.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (index > 0) marginStart = 8.dp
            }
            preset.root.setOnClickListener {
                finishInput()
                viewModel.tipClicked(index)
            }
            presetViews.add(preset)
            binder.swapSettingsPresets.addView(preset.root)
        }
    }

    private fun updatePresetSelection(selectedTip: Int) {
        presetViews.forEachIndexed { index, preset ->
            val selected = index == selectedTip
            preset.swapSlippagePresetLabel.background = requireContext().getRoundedCornerDrawable(
                fillColorRes = if (selected) R.color.button_background_primary else R.color.chips_background,
                cornerSizeInDp = 10
            )
            preset.root.isSelected = selected
        }
    }

    private fun finishInput() {
        hideKeyboard()
        binder.swapSettingsCustomInput.content.clearFocus()
    }
}
