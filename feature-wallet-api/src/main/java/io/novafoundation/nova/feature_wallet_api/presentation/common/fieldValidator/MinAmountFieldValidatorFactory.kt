package io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator

import io.novafoundation.nova.common.validation.FieldValidator
import io.novafoundation.nova.feature_wallet_api.presentation.common.MinAmountProvider
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface MinAmountFieldValidatorFactory {
    fun create(
        chainAsset: Flow<Chain.Asset>,
        minAmountProvider: MinAmountProvider,
        errorMessageRes: Int
    ): MinAmountFieldValidator
}

interface MinAmountFieldValidator : FieldValidator {
    companion object {

        const val ERROR_TAG = "MinAmountValidator"
    }
}
