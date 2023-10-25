package io.novafoundation.nova.feature_swap_impl.presentation.fieldValidation

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.common.validation.MapFieldValidator
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_impl.R

class SlippageFieldValidatorFactory(private val resourceManager: ResourceManager) {

    suspend fun create(slippageConfig: SlippageConfig): SlippageFieldValidator {
        return SlippageFieldValidator(slippageConfig, resourceManager)
    }
}

class SlippageFieldValidator(
    private val slippageConfig: SlippageConfig,
    private val resourceManager: ResourceManager
) : MapFieldValidator() {

    override suspend fun validate(input: String): FieldValidationResult {
        val value = input.toPercent()
        return when {
            input.isEmpty() -> FieldValidationResult.Ok
            value == null -> FieldValidationResult.Error(resourceManager.getString(R.string.common_error_general_title))

            value !in slippageConfig.minAvailableSlippage..slippageConfig.maxAvailableSlippage -> {
                FieldValidationResult.Error(
                    resourceManager.getString(
                        R.string.swap_slippage_error_not_in_available_range,
                        slippageConfig.minAvailableSlippage.format(),
                        slippageConfig.maxAvailableSlippage.format()
                    )
                )
            }

            value < slippageConfig.smallSlippage -> {
                FieldValidationResult.Warning(resourceManager.getString(R.string.swap_slippage_warning_too_small))
            }

            value > slippageConfig.bigSlippage -> {
                FieldValidationResult.Warning(resourceManager.getString(R.string.swap_slippage_warning_too_big))
            }

            else -> FieldValidationResult.Ok
        }
    }

    private fun String.toPercent(): Percent? {
        return toDoubleOrNull()?.let { Percent(it) }
    }
}
