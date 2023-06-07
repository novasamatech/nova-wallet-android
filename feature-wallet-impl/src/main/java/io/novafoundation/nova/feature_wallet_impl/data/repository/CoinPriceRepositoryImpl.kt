package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.common.utils.binarySearchFloor
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.source.CoinPriceLocalDataSource
import io.novafoundation.nova.feature_wallet_api.data.source.CoinPriceRemoteDataSource
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CoinPriceRepositoryImpl(
    private val cacheCoinPriceDataSource: CoinPriceLocalDataSource,
    private val remoteCoinPriceDataSource: CoinPriceRemoteDataSource
) : CoinPriceRepository {

    override suspend fun getCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Long): CoinRate? {
        var coinRate = cacheCoinPriceDataSource.getCoinPriceAtTime(priceId, currency, timestamp)
        if (coinRate == null) {
            val timeInMillis = timestamp.seconds.inWholeMilliseconds
            val coinRateForAllTime = loadAndCacheForAllTime(priceId, currency)
            val index = coinRateForAllTime.binarySearchFloor { it.timestamp.compareTo(timeInMillis) }
            coinRate = coinRateForAllTime.getOrNull(index)
        }

        return coinRate
    }

    override suspend fun getCoinPriceRange(priceId: String, currency: Currency, fromTimestamp: Long, toTimestamp: Long): List<HistoricalCoinRate> {
        var coinRates = cacheCoinPriceDataSource.getCoinPriceRange(priceId, currency, fromTimestamp, toTimestamp)
        if (coinRates.isEmpty()) {
            val fromMillis = fromTimestamp.seconds.inWholeMilliseconds
            val toMillis = toTimestamp.seconds.inWholeMilliseconds
            coinRates = loadAndCacheForAllTime(priceId, currency)
                .filter { it.timestamp in fromMillis..toMillis }
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
        val from = 0L
        val to = System.currentTimeMillis().milliseconds.inWholeSeconds
        val coinRate = remoteCoinPriceDataSource.getCoinPriceRange(priceId, currency, from, to)
        if (coinRate.isNotEmpty()) {
            cacheCoinPriceDataSource.removeAllFor(priceId, currency)
            cacheCoinPriceDataSource.saveCoinPrice(priceId, currency, coinRate)
        }
        return coinRate
    }
}
