package io.novafoundation.nova.feature_currency_api.domain.interfaces

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import kotlinx.coroutines.flow.Flow

interface CurrencyRepository {

    suspend fun syncCurrencies()

    suspend fun getCurrencies(): List<Currency>

    fun observeCurrencies(): Flow<List<Currency>>

    fun observeSelectCurrency(): Flow<Currency>

    suspend fun selectCurrency(currencyId: Int)

    suspend fun getSelectedCurrency(): Currency
}
