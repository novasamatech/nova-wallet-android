package io.novafoundation.nova.feature_swap_impl.presentation.options

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Fraction.Companion.fractions
import io.novafoundation.nova.common.utils.Fraction.Companion.percents
import io.novafoundation.nova.common.utils.formatting.formatPercents
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.feature_swap_api.presentation.model.SlippageSettingsPayload
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.SlippageAlertMixinFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.fieldValidation.SlippageFieldValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed interface SwapSlippageSelection {

    data class Preset(val slippage: Fraction) : SwapSlippageSelection

    data class Custom(val input: String) : SwapSlippageSelection
}

class SwapSettingsSheetViewModel(
    private val swapRouter: SwapRouter,
    private val resourceManager: ResourceManager,
    private val slippageFieldValidator: SlippageFieldValidator,
    slippageAlertMixinFactory: SlippageAlertMixinFactory,
    private val payload: SlippageSettingsPayload,
    private val slippageSettingsCommunicator: SlippageSettingsCommunicator
) : BaseViewModel() {

    private class ValidatedSelection(val selection: SwapSlippageSelection, val validation: FieldValidationResult)

    private val config = payload.config()

    val selection = MutableStateFlow(selectionFor(payload.selected.fractions))

    val slippageTips = config.slippageTips.map { it.formatPercents() }

    val selectedTip = selection.map { selection ->
        when (selection) {
            is SwapSlippageSelection.Preset -> config.slippageTips.indexOfFirst { it.sameAs(selection.slippage) }
            is SwapSlippageSelection.Custom -> -1
        }
    }

    private val validatedSelection = selection.map { selection ->
        ValidatedSelection(selection, slippageFieldValidator.validate(selection.validationInput()))
    }
        .shareInBackground()

    val slippageInputValidationResult = validatedSelection.map { it.validation }

    private val slippageAlertMixin = slippageAlertMixinFactory.create(
        flowOf(config),
        selection.map { it.slippageOrNull() }
    )

    val slippageWarningState = slippageAlertMixin.slippageAlertMessage

    val buttonState = validatedSelection.map { state ->
        val title = resourceManager.getString(R.string.common_apply)
        if (state.selection.slippageOrNull() != null && state.validation is FieldValidationResult.Ok) {
            DescriptiveButtonState.Enabled(title)
        } else {
            DescriptiveButtonState.Disabled(title)
        }
    }

    fun selectionFromInput(input: String): SwapSlippageSelection {
        return if (input.isEmpty()) selectionFor(config.defaultSlippage) else SwapSlippageSelection.Custom(input.replace(',', '.'))
    }

    fun tipClicked(index: Int) {
        selection.value = SwapSlippageSelection.Preset(config.slippageTips[index])
    }

    fun resetClicked() {
        selection.value = selectionFor(config.defaultSlippage)
    }

    fun applyClicked() {
        launch {
            val selected = selection.value
            val slippage = selected.slippageOrNull() ?: return@launch
            if (slippageFieldValidator.validate(selected.validationInput()) is FieldValidationResult.Error) return@launch

            val method = slippageChangeMethod(payload.selected.fractions, slippage, config)
            slippageSettingsCommunicator.respond(SlippageSettingsCommunicator.Response(slippage.inFraction, method, payload.requestId))
            swapRouter.back()
        }
    }

    private fun selectionFor(slippage: Fraction): SwapSlippageSelection {
        return if (config.slippageTips.any { it.sameAs(slippage) }) {
            SwapSlippageSelection.Preset(slippage)
        } else {
            SwapSlippageSelection.Custom(slippageInputText(slippage))
        }
    }

    private fun SwapSlippageSelection.validationInput(): String = when (this) {
        is SwapSlippageSelection.Preset -> slippageInputText(slippage)
        is SwapSlippageSelection.Custom -> input
    }

    private fun SwapSlippageSelection.slippageOrNull(): Fraction? = when (this) {
        is SwapSlippageSelection.Preset -> slippage
        is SwapSlippageSelection.Custom -> input.toDoubleOrNull()?.takeIf { it.isFinite() }?.percents
    }
}
