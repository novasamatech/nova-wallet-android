package io.novafoundation.nova.feature_wallet_impl.presentation.common.fieldValidation

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.feature_wallet_api.presentation.common.MinAmountProvider
import io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator.MinAmountFieldValidator
import io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator.MinAmountFieldValidatorFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RealMinAmountFieldValidatorFactory(
    private val resourceManager: ResourceManager
) : MinAmountFieldValidatorFactory {

    override fun create(
        minAmountProvider: MinAmountProvider,
        errorMessageRes: Int
    ): MinAmountFieldValidator {
        return RealMinAmountFieldValidator(resourceManager, minAmountProvider, errorMessageRes)
    }
}

class RealMinAmountFieldValidator(
    private val resourceManager: ResourceManager,
    private val minAmountProvider: MinAmountProvider,
    private val errorMessageRes: Int,
) : MinAmountFieldValidator {

    override fun observe(inputStream: Flow<String>): Flow<FieldValidationResult> {
        return combine(inputStream, minAmountProvider.provideMinAmount()) { input, minAmount ->
            val inputAmount = input.toBigDecimalOrNull() ?: return@combine FieldValidationResult.Ok

            when {
                minAmount > inputAmount -> FieldValidationResult.Error(
                    reason = resourceManager.getString(errorMessageRes, minAmount),
                    tag = MinAmountFieldValidator.ERROR_TAG
                )

                else -> FieldValidationResult.Ok
            }
        }
    }
}
