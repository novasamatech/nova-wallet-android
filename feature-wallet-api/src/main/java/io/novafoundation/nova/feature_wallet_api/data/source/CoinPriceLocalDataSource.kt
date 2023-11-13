package io.novafoundation.nova.feature_wallet_api.data.source

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate

interface CoinPriceLocalDataSource {

    suspend fun getFloorCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Long): HistoricalCoinRate?

    suspend fun hasCeilingCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Long): Boolean

    suspend fun getCoinPriceRange(priceId: String, currency: Currency, fromTimestamp: Long, toTimestamp: Long): List<HistoricalCoinRate>

    suspend fun updateCoinPrice(priceId: String, currency: Currency, coinRate: List<HistoricalCoinRate>)
}
