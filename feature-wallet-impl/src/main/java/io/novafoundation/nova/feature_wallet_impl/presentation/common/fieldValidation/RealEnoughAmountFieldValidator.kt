package io.novafoundation.nova.feature_wallet_impl.presentation.common.fieldValidation

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator.EnoughAmountFieldValidator
import io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator.EnoughAmountValidatorFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.actualAmount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RealEnoughAmountValidatorFactory(
    private val resourceManager: ResourceManager
) : EnoughAmountValidatorFactory {

    override fun create(
        maxAvailableProvider: MaxActionProvider,
        errorMessageRes: Int
    ): EnoughAmountFieldValidator {
        return RealEnoughAmountFieldValidator(resourceManager, maxAvailableProvider, errorMessageRes)
    }
}

class RealEnoughAmountFieldValidator(
    private val resourceManager: ResourceManager,
    private val maxAvailableProvider: MaxActionProvider,
    private val errorMessageRes: Int,
) : EnoughAmountFieldValidator {

    override fun observe(inputStream: Flow<String>): Flow<FieldValidationResult> {
        return combine(inputStream, maxAvailableProvider.maxAvailableBalance) { input, maxAvailable ->
            val inputAmount = input.toBigDecimalOrNull() ?: return@combine FieldValidationResult.Ok
            val maxAvailableAmount = maxAvailable.actualAmount

            when {
                maxAvailableAmount.isZero || maxAvailableAmount < inputAmount -> FieldValidationResult.Error(
                    reason = resourceManager.getString(errorMessageRes),
                    tag = EnoughAmountFieldValidator.ERROR_TAG
                )

                else -> FieldValidationResult.Ok
            }
        }
    }
}
