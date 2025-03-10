package io.novafoundation.nova.feature_swap_impl.presentation.common.fieldValidation

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.common.validation.FieldValidator
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.actualAmount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class EnoughAmountToSwapValidatorFactory(
    private val resourceManager: ResourceManager
) {

    fun create(maxAvailableProvider: MaxActionProvider): EnoughAmountToSwapFieldValidator {
        return EnoughAmountToSwapFieldValidator(resourceManager, maxAvailableProvider)
    }
}

class EnoughAmountToSwapFieldValidator(
    private val resourceManager: ResourceManager,
    private val maxAvailableProvider: MaxActionProvider,
) : FieldValidator {

    companion object {

        const val ERROR_TAG = "EnoughAmountToSwapFieldValidator"
    }

    override fun observe(inputStream: Flow<String>): Flow<FieldValidationResult> {
        return combine(inputStream, maxAvailableProvider.maxAvailableBalance) { input, maxAvailable ->
            val inputAmount = input.toBigDecimalOrNull() ?: return@combine FieldValidationResult.Ok
            val maxAvailableAmount = maxAvailable.actualAmount

            when {
                maxAvailableAmount.isZero || maxAvailableAmount < inputAmount -> FieldValidationResult.Error(
                    reason = resourceManager.getString(R.string.swap_field_validation_not_enough_amount_to_swap),
                    tag = ERROR_TAG
                )

                else -> FieldValidationResult.Ok
            }
        }
    }
}
