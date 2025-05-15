package io.novafoundation.nova.feature_pay_impl.data.raise.common

import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_currency_api.domain.model.Currency

interface RaiseCurrencyConverter {

    suspend fun convertFromApiCurrency(apiCurrencyCode: String): Currency?
}

suspend fun RaiseCurrencyConverter.convertFromApiCurrencyOrThrow(apiCurrencyCode: String): Currency {
    return requireNotNull(convertFromApiCurrency(apiCurrencyCode)) {
        "Failed to recognize currency $apiCurrencyCode"
    }
}

class RealRaiseCurrencyConverter(
    private val currencyRepository: CurrencyRepository,
) : RaiseCurrencyConverter {

    override suspend fun convertFromApiCurrency(apiCurrencyCode: String): Currency? {
        val allCurrencies = currencyRepository.getCurrencies()

        return allCurrencies.find { it.code == apiCurrencyCode }
    }
}
