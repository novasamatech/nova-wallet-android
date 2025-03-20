package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.common.utils.KeyMutex
import io.novafoundation.nova.common.utils.binarySearchFloor
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.source.CoinPriceLocalDataSource
import io.novafoundation.nova.feature_wallet_api.data.source.CoinPriceRemoteDataSource
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import kotlin.time.Duration

class CoinPriceRepositoryImpl(
    private val cacheCoinPriceDataSource: CoinPriceLocalDataSource,
    private val remoteCoinPriceDataSource: CoinPriceRemoteDataSource
) : CoinPriceRepository {

    private val mutex = KeyMutex()

    override suspend fun getCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Duration): HistoricalCoinRate? {
        val timestampInSeconds = timestamp.inWholeSeconds
        var coinRate = cacheCoinPriceDataSource.getFloorCoinPriceAtTime(priceId, currency, timestampInSeconds)
        val hasCeilingItem = cacheCoinPriceDataSource.hasCeilingCoinPriceAtTime(priceId, currency, timestampInSeconds)
        if (coinRate == null && !hasCeilingItem) {
            val coinRateForAllTime = getLastHistoryForPeriod(priceId, currency, PricePeriod.MAX)
            val index = coinRateForAllTime.binarySearchFloor { it.timestamp.compareTo(timestampInSeconds) }
            coinRate = coinRateForAllTime.getOrNull(index)

            // If nearest coin rate timestamp is bigger than target timestamp it means that coingecko doesn't have data before coin rate timestamp
            // so in this case we should return null
            if (coinRate != null && coinRate.timestamp > timestampInSeconds) {
                return null
            }
        }

        return coinRate
    }

    override suspend fun getLastHistoryForPeriod(priceId: String, currency: Currency, range: PricePeriod): List<HistoricalCoinRate> {
        val key = "${priceId}_${currency.id}_$range"
        return mutex.withKeyLock(key) {
            val days = when (range) {
                PricePeriod.DAY -> "1"
                PricePeriod.WEEK -> "7"
                PricePeriod.MONTH -> "30"
                PricePeriod.YEAR -> "365"
                PricePeriod.MAX -> "max"
            }

            remoteCoinPriceDataSource.getLastCoinPriceRange(priceId, currency, days)
        }
    }
}
