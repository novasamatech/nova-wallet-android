package io.novafoundation.nova.feature_swap_impl.presentation.fieldValidation

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.common.validation.FieldValidator
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.presentation.main.QuotingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LiquidityFieldValidatorFactory(
    private val resourceManager: ResourceManager
) {

    fun create(quotingStateFlow: Flow<QuotingState>): LiquidityFieldValidator {
        return LiquidityFieldValidator(resourceManager, quotingStateFlow)
    }
}

class LiquidityFieldValidator(
    private val resourceManager: ResourceManager,
    private val quotingStateFlow: Flow<QuotingState>
) : FieldValidator {

    override fun observe(inputStream: Flow<String>): Flow<FieldValidationResult> {
        return quotingStateFlow.map { quotingState ->
            if (quotingState is QuotingState.NotAvailable) {
                FieldValidationResult.Error(
                    resourceManager.getString(R.string.swap_field_validation_not_enough_liquidity)
                )
            } else {
                FieldValidationResult.Ok
            }
        }
    }
}
