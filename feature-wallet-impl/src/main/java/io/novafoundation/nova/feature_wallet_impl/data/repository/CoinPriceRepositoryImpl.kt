package io.novafoundation.nova.feature_wallet_impl.data.repository

import android.util.Log
import io.novafoundation.nova.common.utils.binarySearchFloor
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.source.CoinPriceLocalDataSource
import io.novafoundation.nova.feature_wallet_api.data.source.CoinPriceRemoteDataSource
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CoinPriceRepositoryImpl(
    private val cacheCoinPriceDataSource: CoinPriceLocalDataSource, private val remoteCoinPriceDataSource: CoinPriceRemoteDataSource
) : CoinPriceRepository {

    override suspend fun getCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Long): CoinRate? {
        var coinRate = cacheCoinPriceDataSource.getCoinPriceAtTime(priceId, currency, timestamp)
        if (coinRate == null) {
            val timeInMillis = timestamp.seconds.inWholeMilliseconds
            val coinRateForAllTime = loadAndCacheForAllTime(priceId, currency)
            val index = coinRateForAllTime.binarySearchFloor { it.timestamp.compareTo(timeInMillis) }
            coinRate = coinRateForAllTime.getOrNull(index)
            if (coinRate != null && coinRate.timestamp > timestamp) {
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
        if (coinRates.isEmpty() || coinRates.last().timestamp < toTimestamp) {
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

    override fun findNearestCoinRate(coinRates: List<HistoricalCoinRate>, timestamp: Long): HistoricalCoinRate? {
        if (coinRates.isEmpty()) return null
        if (coinRates.first().timestamp > timestamp) return null // To support the case when the token started trading later than the desired coin rate

        val index = coinRates.binarySearchFloor { it.timestamp.compareTo(timestamp) }
        return coinRates.getOrNull(index)
    }

    private suspend fun loadAndCacheForAllTime(priceId: String, currency: Currency): List<HistoricalCoinRate> {
        val from = 0L
        val to = System.currentTimeMillis().milliseconds.inWholeSeconds
        val coinRate = remoteCoinPriceDataSource.getCoinPriceRange(priceId, currency, from, to)
        if (coinRate.isNotEmpty()) {
            cacheCoinPriceDataSource.removeAllFor(priceId, currency)
            cacheCoinPriceDataSource.saveCoinPrice(priceId, currency, coinRate)
        }
        return coinRate
    }

    private fun getOffsetFor(from: Long, to: Long): Pair<Long, Long> {
        val offsetFrom = from.seconds.minus(1.days).inWholeSeconds
        val offsetTo = to.seconds.plus(1.days).inWholeSeconds

        return offsetFrom to offsetTo
    }
}
