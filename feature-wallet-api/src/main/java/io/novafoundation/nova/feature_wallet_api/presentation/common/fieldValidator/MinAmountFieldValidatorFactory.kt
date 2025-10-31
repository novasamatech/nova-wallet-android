package io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator

import io.novafoundation.nova.common.validation.FieldValidator
import io.novafoundation.nova.feature_wallet_api.presentation.common.MinAmountProvider

interface MinAmountFieldValidatorFactory {
    fun create(
        minAmountProvider: MinAmountProvider,
        errorMessageRes: Int
    ): MinAmountFieldValidator
}

interface MinAmountFieldValidator : FieldValidator {
    companion object {

        const val ERROR_TAG = "MinAmountValidator"
    }
}
