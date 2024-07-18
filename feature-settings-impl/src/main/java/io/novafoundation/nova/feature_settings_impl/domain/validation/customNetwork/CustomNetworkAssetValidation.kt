package io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrError

class CustomNetworkAssetValidation<P, F>(
    private val chainMainAssetSymbolRequester: suspend (P) -> String,
    private val symbol: (P) -> String,
    private val failure: (P, String) -> F
) : Validation<P, F> {

    override suspend fun validate(value: P): ValidationStatus<F> {
        val networkSymbol = chainMainAssetSymbolRequester(value)

        return validOrError(networkSymbol == symbol(value)) {
            failure(value, networkSymbol)
        }
    }
}

fun <P, F> ValidationSystemBuilder<P, F>.validateAssetIsMain(
    chainMainAssetSymbolRequester: suspend (P) -> String,
    symbol: (P) -> String,
    failure: (P, String) -> F
) = validate(
    CustomNetworkAssetValidation(chainMainAssetSymbolRequester, symbol, failure)
)
