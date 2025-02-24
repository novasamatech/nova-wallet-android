package io.novafoundation.nova.feature_wallet_api.data.repository

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import retrofit2.HttpException
import kotlin.jvm.Throws
import kotlin.time.Duration

enum class PriceChartPeriod {
    DAY, WEEK, MONTH, YEAR, MAX
}

interface CoinPriceRepository {

    @Throws(HttpException::class)
    suspend fun getCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Duration): HistoricalCoinRate?

    @Throws(HttpException::class)
    suspend fun getCoinPriceRange(priceId: String, currency: Currency, fromTimestamp: Long, toTimestamp: Long): List<HistoricalCoinRate>

    @Throws(HttpException::class)
    suspend fun getCoinRates(priceIds: Set<String>, currency: Currency): Map<String, CoinRateChange?>

    @Throws(HttpException::class)
    suspend fun getCoinRate(priceId: String, currency: Currency): CoinRateChange?

    @Throws(HttpException::class)
    suspend fun getLastCoinPriceRange(priceId: String, currency: Currency, range: PriceChartPeriod): List<HistoricalCoinRate>
}
