package io.novafoundation.nova.feature_swap_impl.presentation.options

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.presentation.DescriptiveButtonState.Disabled
import io.novafoundation.nova.common.presentation.DescriptiveButtonState.Enabled
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Fraction.Companion.fractions
import io.novafoundation.nova.common.utils.Fraction.Companion.percents
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.formatting.formatPercents
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.presentation.model.SlippageSettingsPayload
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description.launchSlippageDescription
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.SlippageAlertMixinFactory
import io.novafoundation.nova.feature_swap_impl.presentation.common.fieldValidation.SlippageFieldValidatorFactory
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class SwapOptionsViewModel(
    private val swapRouter: SwapRouter,
    private val resourceManager: ResourceManager,
    private val swapSettingsStateProvider: SwapSettingsStateProvider,
    private val slippageFieldValidatorFactory: SlippageFieldValidatorFactory,
    private val swapInteractor: SwapInteractor,
    private val descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
    private val slippageAlertMixinFactory: SlippageAlertMixinFactory,
    private val payload: SlippageSettingsPayload?,
    private val slippageSettingsCommunicator: SlippageSettingsCommunicator
) : BaseViewModel(), DescriptionBottomSheetLauncher by descriptionBottomSheetLauncher {

    val title = resourceManager.getString(if (payload == null) R.string.swap_settings_title else R.string.common_settings)

    /** Settings opened with a payload (Bittensor) cap a custom value at four decimals of a percent, as iOS does. */
    val limitsInputPrecision = payload != null

    private val swapSettingState = async(start = CoroutineStart.LAZY) {
        swapSettingsStateProvider.getSwapSettingsState(viewModelScope)
    }

    private val swapSettingsStateFlow = flowOfAll {
        if (payload != null) flowOf(SwapSettings(slippage = payload.selected.fractions)) else swapSettingState.await().selectedOption
    }
        .shareInBackground()

    private val slippageConfig = flowOfAll {
        payload?.let { flowOf(it.config()) } ?: swapSettingsStateFlow
            .mapNotNull { it.assetIn ?: it.assetOut }
            .mapNotNull { swapInteractor.slippageConfig(it.chainId) }
    }
        .shareInBackground()

    val slippageInput = MutableStateFlow("")

    private val slippageFieldValidator = slippageConfig.map { slippageFieldValidatorFactory.create(it) }
        .shareInBackground()

    val slippageInputValidationResult = slippageFieldValidator.flatMapLatest { it.observe(slippageInput) }
        .shareInBackground()

    private val slippageAlertMixin = slippageAlertMixinFactory.create(
        slippageConfig,
        slippageInput.map { it.formatToPercent() }
    )

    val slippageWarningState = slippageAlertMixin.slippageAlertMessage

    val resetButtonEnabled = combine(slippageInput, slippageConfig) { input, slippageConfig ->
        formatResetButtonVisibility(input, slippageConfig)
    }

    val buttonState = combine(slippageInput, swapSettingsStateFlow, slippageInputValidationResult) { input, state, validationStatus ->
        formatButtonState(input, state, validationStatus)
    }

    val defaultSlippage = slippageConfig.map { it.defaultSlippage }
        .map { it.formatPercents() }

    val slippageTips = slippageConfig.map { it.slippageTips }
        .mapList { it.formatPercents() }

    init {
        launch {
            val selectedSlippage = swapSettingsStateFlow.first().slippage
            val defaultSlippage = slippageConfig.first().defaultSlippage
            if (selectedSlippage != defaultSlippage) {
                slippageInput.value = formatInput(selectedSlippage)
            }
        }
    }

    fun slippageInfoClicked() {
        launchSlippageDescription(resourceManager)
    }

    fun tipClicked(index: Int) {
        launch {
            val slippageTips = slippageConfig.first().slippageTips
            slippageInput.value = formatInput(slippageTips[index])
        }
    }

    fun applyClicked() {
        launch {
            val slippage = slippageInput.value.formatToPercent() ?: return@launch
            val config = slippageConfig.first()
            if (slippage !in config.minAvailableSlippage..config.maxAvailableSlippage) return@launch
            if (payload != null) {
                val method = slippageChangeMethod(payload.selected.fractions, slippage, config)
                slippageSettingsCommunicator.respond(SlippageSettingsCommunicator.Response(slippage.inFraction, method))
            } else {
                swapSettingState.await().setSlippage(slippage)
            }
            swapRouter.back()
        }
    }

    fun resetClicked() {
        slippageInput.value = ""
    }

    fun backClicked() {
        swapRouter.back()
    }

    // Payload settings save a locale-independent value the precision filter accepts; the swap flow keeps its format.
    private fun formatInput(slippage: Fraction): String {
        return if (payload != null) slippageInputText(slippage) else slippage.formatPercents(includeSymbol = false)
    }

    private suspend fun String.formatToPercent(): Fraction? {
        val defaultSlippage = slippageConfig.first().defaultSlippage

        return if (isEmpty()) {
            defaultSlippage
        } else {
            return toDoubleOrNull()?.takeIf { it.isFinite() }?.percents
        }
    }

    private suspend fun formatButtonState(
        insertedSlippage: String,
        settings: SwapSettings,
        validationResult: FieldValidationResult
    ): DescriptiveButtonState {
        val slippage = insertedSlippage.formatToPercent()
        return when {
            slippage == null -> Disabled(resourceManager.getString(R.string.swap_slippage_disabled_button_state))
            validationResult is FieldValidationResult.Error -> Disabled(resourceManager.getString(R.string.swap_slippage_disabled_button_state))
            slippage != settings.slippage -> Enabled(resourceManager.getString(R.string.common_apply))
            else -> Disabled(resourceManager.getString(R.string.common_apply))
        }
    }

    private suspend fun formatResetButtonVisibility(slippageInput: String, slippageConfig: SlippageConfig): Boolean {
        return slippageInput.formatToPercent() != slippageConfig.defaultSlippage
    }
}
