package io.novafoundation.nova.feature_swap_impl.presentation.common.fieldValidation

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.common.validation.FieldValidator
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal

class EnoughAmountToSwapValidatorFactory(
    private val resourceManager: ResourceManager
) {

    fun create(assetFlow: Flow<Asset?>): EnoughAmountToSwapFieldValidator {
        return EnoughAmountToSwapFieldValidator(resourceManager, assetFlow)
    }
}

class EnoughAmountToSwapFieldValidator(
    private val resourceManager: ResourceManager,
    private val assetFlow: Flow<Asset?>
) : FieldValidator {

    override fun observe(inputStream: Flow<String>): Flow<FieldValidationResult> {
        return combine(inputStream, assetFlow) { input, asset ->
            val amount = input.toBigDecimalOrNull() ?: return@combine FieldValidationResult.Ok
            asset ?: return@combine FieldValidationResult.Ok

            when {
                amount == BigDecimal.ZERO -> FieldValidationResult.Ok

                asset.transferable < amount -> {
                    FieldValidationResult.Error(
                        resourceManager.getString(R.string.swap_field_validation_not_enough_amount_to_swap)
                    )
                }

                else -> FieldValidationResult.Ok
            }
        }
    }
}
