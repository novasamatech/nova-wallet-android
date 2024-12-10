package io.novafoundation.nova.feature_swap_impl.presentation.common.fieldValidation

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Fraction.Companion.percents
import io.novafoundation.nova.common.utils.formatting.formatPercents
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.common.validation.MapFieldValidator
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_impl.R

class SlippageFieldValidatorFactory(private val resourceManager: ResourceManager) {

    fun create(slippageConfig: SlippageConfig): SlippageFieldValidator {
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

            value == null -> FieldValidationResult.Ok

            value !in slippageConfig.minAvailableSlippage..slippageConfig.maxAvailableSlippage -> {
                FieldValidationResult.Error(
                    resourceManager.getString(
                        R.string.swap_slippage_error_not_in_available_range,
                        slippageConfig.minAvailableSlippage.formatPercents(),
                        slippageConfig.maxAvailableSlippage.formatPercents()
                    )
                )
            }

            else -> FieldValidationResult.Ok
        }
    }

    private fun String.toPercent(): Fraction? {
        return toDoubleOrNull()?.percents
    }
}
