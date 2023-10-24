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
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.AssetConversionExchangeFactory
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.fieldValidation.SlippageFieldValidatorFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class SwapOptionsViewModel(
    private val swapRouter: SwapRouter,
    private val resourceManager: ResourceManager,
    private val swapSettingsStateProvider: SwapSettingsStateProvider,
    private val assetExchangeFactory: AssetConversionExchangeFactory,
    private val slippageFieldValidatorFactory: SlippageFieldValidatorFactory
) : BaseViewModel() {

    private val swapSettingState = async {
        swapSettingsStateProvider.getSwapSettingsState(viewModelScope)
    }

    private val swapSettingsStateFlow = flowOfAll { swapSettingState.await().selectedOption }
        .shareInBackground()

    private val assetExchange = swapSettingsStateFlow.mapNotNull { it.assetIn }
        .map { assetExchangeFactory.create(it.chainId) }
        .filterNotNull()
        .shareInBackground()

    private val slippageConfig = assetExchange.map { it.slippageConfig() }
        .shareInBackground()

    val slippageInput = MutableStateFlow("")

    private val slippageFieldValidator = assetExchange.map { slippageFieldValidatorFactory.create(it) }
        .shareInBackground()

    private val slippageInputValidationResult = slippageFieldValidator.flatMapLatest { it.observe(slippageInput) }
        .shareInBackground()

    val slippageWarningState = slippageInputValidationResult.map { formatSlippageWarning(it) }

    val slippageErrorState = slippageInputValidationResult.map { formatSlippageError(it) }

    val resetButtonEnabled = combine(slippageInput, swapSettingsStateFlow) { input, settings ->
        formatResetButtonVisibility(input, settings)
    }

    val buttonState = combine(slippageInput, swapSettingsStateFlow, slippageInputValidationResult) { input, state, validationStatus ->
        formatButtonState(input, state, validationStatus)
    }

    val defaultSlippage = slippageConfig.map { it.defaultSlippage }
        .map { it.format() }

    val slippageTips = slippageConfig.map { it.slippageTips }
        .map { it.map { it.format() } }

    init {
        launch {
            val selectedSlippage = swapSettingsStateFlow.first().slippage
            val defaultSlippage = slippageConfig.first().defaultSlippage
            if (selectedSlippage != defaultSlippage) {
                slippageInput.value = selectedSlippage.formatWithoutSymbol()
            }
        }
    }

    fun tipClicked(index: Int) {
        launch {
            val slippageTips = slippageConfig.first().slippageTips
            slippageInput.value = slippageTips[index].formatWithoutSymbol()
        }
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

    private suspend fun String.formatToPercent(): Percent {
        val defaultSlippage = slippageConfig.first().defaultSlippage
        return if (isEmpty()) {
            defaultSlippage
        } else {
            return Percent(this.toDouble())
        }
    }

    private fun formatSlippageError(validationResult: FieldValidationResult): String? {
        if (validationResult is FieldValidationResult.Error) {
            return validationResult.reason
        }

        return null
    }

    private fun formatSlippageWarning(validationResult: FieldValidationResult): String? {
        if (validationResult is FieldValidationResult.Warning) {
            return validationResult.reason
        }

        return null
    }

    private suspend fun formatButtonState(
        insertedSlippage: String,
        settings: SwapSettings,
        validationResult: FieldValidationResult
    ): DescriptiveButtonState {
        val slippage = insertedSlippage.formatToPercent()
        return when {
            validationResult is FieldValidationResult.Error -> Disabled(resourceManager.getString(R.string.swap_slippage_disabled_button_state))
            slippage != settings.slippage -> Enabled(resourceManager.getString(R.string.common_apply))
            else -> Disabled(resourceManager.getString(R.string.common_apply))
        }
    }

    private suspend fun formatResetButtonVisibility(slippageInput: String, settings: SwapSettings): Boolean {
        return slippageInput.formatToPercent() != settings.slippage
    }
}
