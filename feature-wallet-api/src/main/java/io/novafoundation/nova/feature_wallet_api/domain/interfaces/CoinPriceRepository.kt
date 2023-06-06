package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate

interface CoinPriceRepository {

    suspend fun getCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Long): CoinRate?

    suspend fun getCoinPriceRange(priceId: String, currency: Currency, fromTimestamp: Long, toTimestamp: Long): List<HistoricalCoinRate>

    suspend fun getCoinRates(priceIds: Set<String>, currency: Currency): Map<String, CoinRateChange?>

    suspend fun getCoinRate(priceId: String, currency: Currency): CoinRateChange?
}
