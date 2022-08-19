package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.feature_wallet_api.domain.model.Currency
import kotlinx.coroutines.flow.Flow

interface CurrencyRepository {

    suspend fun syncCurrencies()

    suspend fun observeCurrencies(): Flow<List<Currency>>

    suspend fun observeSelectCurrency(): Flow<Currency>

    suspend fun selectCurrency(currencyId: Int)
}
