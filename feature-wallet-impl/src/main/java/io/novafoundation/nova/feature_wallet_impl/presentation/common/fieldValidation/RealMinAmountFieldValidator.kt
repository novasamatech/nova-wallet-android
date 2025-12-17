package io.novafoundation.nova.feature_wallet_impl.presentation.common.fieldValidation

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.feature_wallet_api.presentation.common.MinAmountProvider
import io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator.MinAmountFieldValidator
import io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator.MinAmountFieldValidatorFactory
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class RealMinAmountFieldValidatorFactory(
    private val resourceManager: ResourceManager,
    private val tokenFormatter: TokenFormatter,
) : MinAmountFieldValidatorFactory {

    override fun create(
        chainAsset: Flow<Chain.Asset>,
        minAmountProvider: MinAmountProvider,
        errorMessageRes: Int
    ): MinAmountFieldValidator {
        return RealMinAmountFieldValidator(resourceManager, chainAsset, tokenFormatter, minAmountProvider, errorMessageRes)
    }
}

class RealMinAmountFieldValidator(
    private val resourceManager: ResourceManager,
    private val chainAssetFlow: Flow<Chain.Asset>,
    private val tokenFormatter: TokenFormatter,
    private val minAmountProvider: MinAmountProvider,
    private val errorMessageRes: Int,
) : MinAmountFieldValidator {

    override fun observe(inputStream: Flow<String>): Flow<FieldValidationResult> {
        return combine(inputStream, minAmountProvider.provideMinAmount()) { input, minAmount ->
            val inputAmount = input.toBigDecimalOrNull() ?: return@combine FieldValidationResult.Ok

            when {
                minAmount > inputAmount -> {
                    val chainAsset = chainAssetFlow.first()
                    FieldValidationResult.Error(
                        reason = resourceManager.getString(errorMessageRes, tokenFormatter.formatToken(minAmount, chainAsset.symbol)),
                        tag = MinAmountFieldValidator.ERROR_TAG
                    )
                }

                else -> FieldValidationResult.Ok
            }
        }
    }
}
