package io.novafoundation.nova.feature_wallet_api.data.source

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate

interface CoinPriceRemoteDataSource {

    suspend fun getLastCoinPriceRange(priceId: String, currency: Currency, days: String): List<HistoricalCoinRate>

    suspend fun getCoinRates(priceIds: Set<String>, currency: Currency): Map<String, CoinRateChange?>

    suspend fun getCoinRate(priceId: String, currency: Currency): CoinRateChange?
}
