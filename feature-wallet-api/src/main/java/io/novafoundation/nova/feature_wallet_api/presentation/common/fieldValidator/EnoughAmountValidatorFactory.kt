package io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator

import io.novafoundation.nova.common.validation.FieldValidator
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider

interface EnoughAmountValidatorFactory {
    fun create(
        maxAvailableProvider: MaxActionProvider,
        errorMessageRes: Int = R.string.common_error_not_enough_tokens
    ): EnoughAmountFieldValidator
}

interface EnoughAmountFieldValidator : FieldValidator {
    companion object {

        const val ERROR_TAG = "EnoughAmountFieldValidator"
    }
}
