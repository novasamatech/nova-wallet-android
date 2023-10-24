package io.novafoundation.nova.feature_swap_impl.presentation.fieldValidation

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.common.validation.FieldValidator
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SlippageFieldValidatorFactory(private val resourceManager: ResourceManager) {

    suspend fun create(assetExchange: AssetExchange): SlippageFieldValidator {
        return SlippageFieldValidator(assetExchange, resourceManager)
    }
}

class SlippageFieldValidator(
    private val assetExchange: AssetExchange,
    private val resourceManager: ResourceManager
) : FieldValidator {

    override fun observe(inputStream: Flow<String>): Flow<FieldValidationResult> {
        return inputStream.map {
            val slippageConfig = assetExchange.slippageConfig()
            val value = it.toPercent()
            when {
                it.isEmpty() -> FieldValidationResult.Ok
                value == null -> FieldValidationResult.Error(resourceManager.getString(R.string.common_error_general_title))

                value < slippageConfig.minAvailableSlippage || value > slippageConfig.maxAvailableSlippage -> {
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

                else -> {
                    FieldValidationResult.Ok
                }
            }
        }
    }

    private fun String.toPercent(): Percent? {
        return toDoubleOrNull()?.let { Percent(it) }
    }
}
