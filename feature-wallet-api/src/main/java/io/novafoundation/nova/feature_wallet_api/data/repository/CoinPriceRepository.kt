package io.novafoundation.nova.feature_wallet_api.data.repository

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import retrofit2.HttpException
import kotlin.jvm.Throws
import kotlin.time.Duration

interface CoinPriceRepository {

    @Throws(HttpException::class)
    suspend fun getCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Duration): HistoricalCoinRate?

    @Throws(HttpException::class)
    suspend fun getLastHistoryForPeriod(priceId: String, currency: Currency, range: PricePeriod): List<HistoricalCoinRate>
}

@Throws(HttpException::class)
suspend fun CoinPriceRepository.getAllCoinPriceHistory(priceId: String, currency: Currency): List<HistoricalCoinRate> {
    return getLastHistoryForPeriod(priceId, currency, PricePeriod.MAX)
}
