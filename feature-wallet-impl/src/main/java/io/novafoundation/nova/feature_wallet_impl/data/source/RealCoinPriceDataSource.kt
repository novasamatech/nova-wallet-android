package io.novafoundation.nova.feature_wallet_impl.data.source

import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.utils.KeyMutex
import io.novafoundation.nova.common.utils.asQueryParam
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.network.priceApi.CoingeckoApi
import io.novafoundation.nova.feature_wallet_api.data.network.priceApi.ProxyPriceApi
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.data.source.CoinPriceRemoteDataSource
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import kotlin.time.Duration.Companion.milliseconds

class RealCoinPriceDataSource(
    private val priceApi: ProxyPriceApi,
    private val coingeckoApi: CoingeckoApi,
    private val httpExceptionHandler: HttpExceptionHandler
) : CoinPriceRemoteDataSource {

    private val mutex = KeyMutex()

    override suspend fun getLastCoinPriceRange(priceId: String, currency: Currency, range: PricePeriod): List<HistoricalCoinRate> {
        val key = "${priceId}_${currency.id}_$range"
        val response = mutex.withKeyLock(key) {
            val days = mapRangeToDays(range)

            priceApi.getLastCoinRange(priceId, currency.coingeckoId, days)
        }

        return response.prices.map { (timestampRaw, rateRaw) ->
            HistoricalCoinRate(
                timestamp = timestampRaw.toLong().milliseconds.inWholeSeconds,
                rate = rateRaw
            )
        }
    }

    override suspend fun getCoinRates(priceIds: Set<String>, currency: Currency): Map<String, CoinRateChange?> {
        val sortedPriceIds = priceIds.toList().sorted()
        return apiCall { coingeckoApi.getAssetPrice(sortedPriceIds.asQueryParam(), currency = currency.coingeckoId, includeRateChange = true) }
            .mapValues {
                val price = it.value[currency.coingeckoId].orZero()
                val recentRate = it.value[CoingeckoApi.getRecentRateFieldName(currency.coingeckoId)].orZero()
                CoinRateChange(
                    recentRate.toBigDecimal(),
                    price.toBigDecimal()
                )
            }
    }

    override suspend fun getCoinRate(priceId: String, currency: Currency): CoinRateChange? {
        return getCoinRates(priceIds = setOf(priceId), currency = currency)
            .values
            .firstOrNull()
    }

    private suspend fun <T> apiCall(block: suspend () -> T): T = httpExceptionHandler.wrap(block)

    private fun mapRangeToDays(range: PricePeriod) = when (range) {
        PricePeriod.DAY -> "1"
        PricePeriod.WEEK -> "7"
        PricePeriod.MONTH -> "30"
        PricePeriod.YEAR -> "365"
        PricePeriod.MAX -> "max"
    }
}
