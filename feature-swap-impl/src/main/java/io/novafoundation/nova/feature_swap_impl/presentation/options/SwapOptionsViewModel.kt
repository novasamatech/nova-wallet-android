package io.novafoundation.nova.feature_swap_impl.presentation.options

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.presentation.DescriptiveButtonState.Disabled
import io.novafoundation.nova.common.presentation.DescriptiveButtonState.Enabled
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.formatting.formatWithoutSymbol
import io.novafoundation.nova.common.validation.InputValidationMixinFactory
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isError
import io.novafoundation.nova.common.validation.isWarning
import io.novafoundation.nova.common.validation.notValidOrNull
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.domain.slippage.SlippageRepository
import io.novafoundation.nova.feature_swap_impl.domain.validation.slippage.SlippageValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.slippage.SlippageValidationPayload
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SwapOptionsViewModel(
    private val swapRouter: SwapRouter,
    private val resourceManager: ResourceManager,
    private val swapSettingsStateProvider: SwapSettingsStateProvider,
    private val inputValidationMixinFactory: InputValidationMixinFactory<SlippageValidationPayload, SlippageValidationFailure>,
    private val slippageRepository: SlippageRepository
) : BaseViewModel() {

    private val defaultSlippage = slippageRepository.getDefaultSlippage()
    private val slippageTips = slippageRepository.getSlippageTips()

    private val swapSettingState = async {
        swapSettingsStateProvider.getSwapSettingsState(viewModelScope)
    }

    private val swapSettingsStateFlow = flowOfAll { swapSettingState.await().selectedOption }
        .shareInBackground()

    val slippageInput = MutableStateFlow("")

    private val slippageInputValidationStatus = inputValidationMixinFactory.create(slippageInput, ::formatSlippagePayload)
        .observeStatus()
        .shareInBackground()

    private fun formatSlippagePayload(text: String): SlippageValidationPayload {
        return SlippageValidationPayload(text.formatToPercent())
    }

    val slippageWarningState = slippageInputValidationStatus.map { formatSlippageWarning(it) }

    val slippageErrorState = slippageInputValidationStatus.map { formatSlippageError(it) }

    val resetButtonEnabled = combine(slippageInput, swapSettingsStateFlow) { input, settings ->
        formatResetButtonVisibility(input, settings)
    }

    val buttonState = combine(slippageInput, swapSettingsStateFlow, slippageInputValidationStatus) { input, state, validationStatus ->
        formatButtonState(input, state, validationStatus)
    }

    init {
        launch {
            val selectedSlippage = swapSettingsStateFlow.first().slippage
            if (selectedSlippage != defaultSlippage) {
                slippageInput.value = selectedSlippage.formatWithoutSymbol()
            }
        }
    }

    fun getDefaultSlippage(): String {
        return defaultSlippage.format()
    }

    fun getSlippageTips(): List<String> {
        return slippageTips.map { it.format() }
    }

    fun tipClicked(index: Int) {
        slippageInput.value = slippageTips[index].formatWithoutSymbol()
    }

    fun applyClicked() {
        launch {
            val slippage = slippageInput.value.formatToPercent()
            swapSettingState.await().setSlippage(slippage)
            swapRouter.back()
        }
    }

    fun resetClicked() {
        slippageInput.value = ""
    }

    fun backClicked() {
        backClicked()
    }

    private fun String.formatToPercent(): Percent {
        return if (isEmpty()) {
            defaultSlippage
        } else {
            return Percent(this.toDouble())
        }
    }

    private fun formatSlippageError(validationResult: Result<ValidationStatus<SlippageValidationFailure>>): String? {
        val errorReason = validationResult.getOrNull()
            ?.notValidOrNull()
            ?.takeIf { it.isError() }
            ?.reason
            ?: return null

        return when (errorReason) {
            is SlippageValidationFailure.NotInAvailableRange -> {
                resourceManager.getString(
                    R.string.swap_slippage_error_not_in_available_range,
                    errorReason.minSlippage.format(),
                    errorReason.maxSlippage.format()
                )
            }

            else -> null
        }
    }

    private fun formatSlippageWarning(validationResult: Result<ValidationStatus<SlippageValidationFailure>>): String? {
        val warningReason = validationResult.getOrNull()
            ?.notValidOrNull()
            ?.takeIf { it.isWarning() }
            ?.reason
            ?: return null

        return when (warningReason) {
            SlippageValidationFailure.TooSmall -> {
                resourceManager.getString(R.string.swap_slippage_warning_too_small)
            }

            SlippageValidationFailure.TooBig -> {
                resourceManager.getString(R.string.swap_slippage_warning_too_big)
            }

            else -> null
        }
    }

    private fun formatButtonState(
        insertedSlippage: String,
        settings: SwapSettings,
        validationResult: Result<ValidationStatus<SlippageValidationFailure>>
    ): DescriptiveButtonState {
        val validationFailure = validationResult.getOrNull() as? ValidationStatus.NotValid
        return if (validationFailure?.isError() == true) {
            Disabled(resourceManager.getString(R.string.swap_slippage_disabled_button_state))
        } else {
            val slippage = insertedSlippage.formatToPercent()
            if (slippage != settings.slippage) {
                Enabled(resourceManager.getString(R.string.common_apply))
            } else {
                Disabled(resourceManager.getString(R.string.common_apply))
            }
        }
    }

    private fun formatResetButtonVisibility(slippageInput: String, settings: SwapSettings): Boolean {
        return slippageInput.formatToPercent() != settings.slippage
    }
}
