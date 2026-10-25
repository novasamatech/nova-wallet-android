package io.novafoundation.nova.feature_swap_impl.presentation.options

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.presentation.DescriptiveButtonState.Disabled
import io.novafoundation.nova.common.presentation.DescriptiveButtonState.Enabled
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Fraction.Companion.fractions
import io.novafoundation.nova.common.utils.Fraction.Companion.percents
import io.novafoundation.nova.common.utils.formatting.formatPercents
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.presentation.model.SlippageSettingsPayload
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.SlippageAlertMixinFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.fieldValidation.SlippageFieldValidatorFactory
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SlippageSettingsViewModel(
    private val swapRouter: SwapRouter,
    private val resourceManager: ResourceManager,
    private val slippageFieldValidatorFactory: SlippageFieldValidatorFactory,
    private val slippageAlertMixinFactory: SlippageAlertMixinFactory,
    private val slippageSettingsCommunicator: SlippageSettingsCommunicator,
    private val payload: SlippageSettingsPayload
) : BaseViewModel() {

    private val slippageConfig = payload.config()

    private val initialSlippage = payload.selected.fractions

    val slippageInput = MutableStateFlow("")

    // A preset is selected only while the custom field is empty.
    private val selectedPresetIndex = MutableStateFlow<Int?>(null)

    private val selectedPreset = combine(selectedPresetIndex, slippageInput) { index, input -> index.takeIf { input.isEmpty() } }

    // The value Done applies: the selected preset, or else the field.
    private val slippageValue = combine(selectedPreset, slippageInput) { preset, input ->
        preset?.let(slippageConfig.slippageTips::getOrNull) ?: input.toSlippage()
    }

    val slippageInputValidationResult = slippageFieldValidatorFactory.create(slippageConfig).observe(slippageInput)
        .shareInBackground()

    private val slippageAlertMixin = slippageAlertMixinFactory.create(flowOf(slippageConfig), slippageValue)

    val slippageWarningState = slippageAlertMixin.slippageAlertMessage

    val buttonState = combine(slippageValue, slippageInputValidationResult, ::formatButtonState)

    val slippagePresets = selectedPreset.map { selected ->
        slippageConfig.slippageTips.mapIndexed { index, tip -> SlippagePresetModel(tip.formatPercents(), isSelected = index == selected) }
    }
        .shareInBackground()

    private var slippageResponded = false

    init {
        // A value equal to a preset shows that preset selected and the custom field empty.
        val presetIndex = slippageConfig.slippageTips.indexOfFirst(initialSlippage::sameAs)
        if (presetIndex >= 0) selectedPresetIndex.value = presetIndex else slippageInput.value = slippageInputText(initialSlippage)

        // Typing a custom value deselects the preset, so emptying the field again leaves no value.
        slippageInput
            .onEach { if (it.isNotEmpty()) selectedPresetIndex.value = null }
            .launchIn(this)
    }

    fun presetClicked(index: Int) {
        slippageInput.value = ""
        selectedPresetIndex.value = index
    }

    fun applyClicked() {
        launch {
            val slippage = slippageValue.first() ?: return@launch
            if (slippage !in slippageConfig.minAvailableSlippage..slippageConfig.maxAvailableSlippage) return@launch
            // The first Done closes the sheet; a second click already queued behind it would pop the screen under the sheet.
            if (slippageResponded) return@launch
            slippageResponded = true

            val method = slippageChangeMethod(initialSlippage, slippage, slippageConfig)
            slippageSettingsCommunicator.respond(SlippageSettingsCommunicator.Response(slippage.inFraction, method))
            swapRouter.back()
        }
    }

    // An empty field is no value and Done applies nothing.
    private fun String.toSlippage(): Fraction? = toDoubleOrNull()?.takeIf { it.isFinite() }?.percents

    // A single Done action, which keeps its title while disabled.
    private fun formatButtonState(slippage: Fraction?, validationResult: FieldValidationResult): DescriptiveButtonState {
        val label = resourceManager.getString(R.string.common_done)
        val canApply = slippage != null && validationResult !is FieldValidationResult.Error && slippage != initialSlippage
        return if (canApply) Enabled(label) else Disabled(label)
    }
}

class SlippagePresetModel(val title: String, val isSelected: Boolean)

/** A plain, locale-independent percent with at most [SLIPPAGE_MAX_FRACTION_DIGITS] decimals, e.g. 0.0125. */
internal fun slippageInputText(slippage: Fraction): String = BigDecimal(slippage.inPercents)
    .setScale(SLIPPAGE_MAX_FRACTION_DIGITS, RoundingMode.HALF_UP)
    .stripTrailingZeros()
    .toPlainString()

/** Back to the default is a reset, a listed tip is a preset, anything else is custom. */
internal fun slippageChangeMethod(initial: Fraction, selected: Fraction, config: SlippageConfig): SlippageSettingsCommunicator.Method = when {
    selected.sameAs(config.defaultSlippage) && !initial.sameAs(config.defaultSlippage) -> SlippageSettingsCommunicator.Method.RESET
    config.slippageTips.any(selected::sameAs) -> SlippageSettingsCommunicator.Method.PRESET
    else -> SlippageSettingsCommunicator.Method.CUSTOM
}

// Typed percents and configured fractions take different floating-point paths, e.g. 0.1 / 100 != 0.001.
private fun Fraction.sameAs(other: Fraction): Boolean = abs(inFraction - other.inFraction) < FRACTION_EPSILON

private const val FRACTION_EPSILON = 1e-12
