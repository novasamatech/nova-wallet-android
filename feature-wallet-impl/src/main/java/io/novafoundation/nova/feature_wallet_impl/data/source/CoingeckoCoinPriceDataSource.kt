package io.novafoundation.nova.feature_wallet_impl.data.source

import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.utils.asQueryParam
import io.novafoundation.nova.common.utils.second
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.network.coingecko.CoingeckoApi
import io.novafoundation.nova.feature_wallet_api.data.source.CoinPriceDataSource
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class CoingeckoCoinPriceDataSource(
    private val coingeckoApi: CoingeckoApi,
    private val httpExceptionHandler: HttpExceptionHandler
) : CoinPriceDataSource {

    override suspend fun getCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Long): CoinRate? {
        val from = timestamp.seconds
            .minus(5.minutes)
            .inWholeSeconds
        val to = timestamp.seconds
            .plus(5.minutes)
            .inWholeSeconds
        return getCoinPriceRange(priceId, currency, from, to).firstOrNull()
    }

    override suspend fun getCoinPriceRange(priceId: String, currency: Currency, fromTimestamp: Long, toTimestamp: Long): List<HistoricalCoinRate> {
        val response = coingeckoApi.getCoinRange(priceId, currency.coingeckoId, fromTimestamp, toTimestamp)
        return response.prices.map { HistoricalCoinRate(it.first().toLong(), it.second()) }
    }

    override suspend fun getCoinRates(priceIds: Set<String>, currency: Currency): Map<String, CoinRateChange?> {
        return apiCall { coingeckoApi.getAssetPrice(priceIds.asQueryParam(), currency = currency.coingeckoId, includeRateChange = true) }
            .mapValues {
                val price = it.value[currency.coingeckoId] ?: return@mapValues null
                val recentRate = it.value[CoingeckoApi.getRecentRateFieldName(currency.coingeckoId)] ?: return@mapValues null
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
}
