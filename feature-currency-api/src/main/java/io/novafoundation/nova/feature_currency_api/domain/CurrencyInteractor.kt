package io.novafoundation.nova.feature_currency_api.domain

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import kotlinx.coroutines.flow.Flow

interface CurrencyInteractor {

    suspend fun syncCurrencies()

    fun observeCurrencies(): Flow<GroupedList<CurrencyCategory, Currency>>

    fun observeSelectCurrency(): Flow<Currency>

    suspend fun getSelectedCurrency(): Currency

    suspend fun selectCurrency(currencyId: Int)
}
