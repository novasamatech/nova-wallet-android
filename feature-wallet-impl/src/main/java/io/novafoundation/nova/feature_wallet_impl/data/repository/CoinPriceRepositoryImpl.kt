package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.common.utils.binarySearchFloor
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.source.CoinPriceLocalDataSource
import io.novafoundation.nova.feature_wallet_api.data.source.CoinPriceRemoteDataSource
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val MAX_ALLOWED_PRICE_RANGE = 365.days
private const val BASE_OFFSET = 1

class CoinPriceRepositoryImpl(
    private val cacheCoinPriceDataSource: CoinPriceLocalDataSource,
    private val remoteCoinPriceDataSource: CoinPriceRemoteDataSource
) : CoinPriceRepository {

    override suspend fun getCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Duration): HistoricalCoinRate? {
        val timestampInSeconds = timestamp.inWholeSeconds
        var coinRate = cacheCoinPriceDataSource.getFloorCoinPriceAtTime(priceId, currency, timestampInSeconds)
        val hasCeilingItem = cacheCoinPriceDataSource.hasCeilingCoinPriceAtTime(priceId, currency, timestampInSeconds)
        if (coinRate == null && !hasCeilingItem) {
            val timeInMillis = timestamp.inWholeMilliseconds
            val coinRateForAllTime = loadAndCacheForAllTime(priceId, currency)
            val index = coinRateForAllTime.binarySearchFloor { it.timestamp.compareTo(timeInMillis) }
            coinRate = coinRateForAllTime.getOrNull(index)

            // If nearest coin rate timestamp is bigger than target timestamp it means that coingecko doesn't have data before coin rate timestamp
            // so in this case we should return null
            if (coinRate != null && coinRate.timestamp > timestampInSeconds) {
                return null
            }
        }

        return coinRate
    }

    override suspend fun getCoinPriceRange(priceId: String, currency: Currency, fromTimestamp: Long, toTimestamp: Long): List<HistoricalCoinRate> {
        // To avoid missing data in case when 'from' and 'to' are the same value,
        // we need to get data from 1 day before and after
        val (offsetFrom, offsetTo) = getOffsetFor(fromTimestamp, toTimestamp)

        var coinRates = cacheCoinPriceDataSource.getCoinPriceRange(priceId, currency, offsetFrom, offsetTo)
        val hasCeilingItem = cacheCoinPriceDataSource.hasCeilingCoinPriceAtTime(priceId, currency, offsetTo)
        val shouldUpdateCache = coinRates.isEmpty() && !hasCeilingItem
        if (shouldUpdateCache || timestampIsHigherThanCoinRate(coinRates.lastOrNull(), toTimestamp)) {
            val timeRange = offsetFrom..offsetTo
            coinRates = loadAndCacheForAllTime(priceId, currency).filter { it.timestamp in timeRange }
        }

        return coinRates
    }

    override suspend fun getCoinRates(priceIds: Set<String>, currency: Currency): Map<String, CoinRateChange?> {
        return remoteCoinPriceDataSource.getCoinRates(priceIds, currency)
    }

    override suspend fun getCoinRate(priceId: String, currency: Currency): CoinRateChange? {
        return remoteCoinPriceDataSource.getCoinRate(priceId, currency)
    }

    private suspend fun loadAndCacheForAllTime(priceId: String, currency: Currency): List<HistoricalCoinRate> {
        val to = System.currentTimeMillis().milliseconds
        val from = to - MAX_ALLOWED_PRICE_RANGE
        val coinRate = remoteCoinPriceDataSource.getCoinPriceRange(priceId, currency, from.inWholeSeconds, to.inWholeSeconds)
        if (coinRate.isNotEmpty()) {
            cacheCoinPriceDataSource.updateCoinPrice(priceId, currency, coinRate)
        }
        return coinRate
    }

    private fun getOffsetFor(from: Long, to: Long): Pair<Long, Long> {
        val offsetFrom = from.seconds.minus(BASE_OFFSET.days).inWholeSeconds
        val offsetTo = to.seconds.plus(BASE_OFFSET.days).inWholeSeconds

        return offsetFrom to offsetTo
    }

    private fun timestampIsHigherThanCoinRate(coinRate: HistoricalCoinRate?, timestamp: Long): Boolean {
        if (coinRate == null) return false
        val coinRateTimestampWithOffset = coinRate.timestamp.plus(BASE_OFFSET.days.inWholeSeconds)
        return timestamp >= coinRateTimestampWithOffset
    }
}
