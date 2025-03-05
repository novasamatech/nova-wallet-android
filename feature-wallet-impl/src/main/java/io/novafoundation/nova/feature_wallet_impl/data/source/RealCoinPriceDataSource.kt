package io.novafoundation.nova.feature_wallet_impl.data.source

import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.utils.asQueryParam
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.network.coingecko.PriceApi
import io.novafoundation.nova.feature_wallet_api.data.source.CoinPriceRemoteDataSource
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import kotlin.time.Duration.Companion.milliseconds

class RealCoinPriceDataSource(
    private val priceApi: PriceApi,
    private val httpExceptionHandler: HttpExceptionHandler
) : CoinPriceRemoteDataSource {

    override suspend fun getCoinPriceRange(priceId: String, currency: Currency, fromTimestamp: Long, toTimestamp: Long): List<HistoricalCoinRate> {
        val response = priceApi.getCoinRange(priceId, currency.coingeckoId, fromTimestamp, toTimestamp)

        return response.prices.map { (timestampRaw, rateRaw) ->
            HistoricalCoinRate(
                timestamp = timestampRaw.toLong().milliseconds.inWholeSeconds,
                rate = rateRaw
            )
        }
    }

    override suspend fun getLastCoinPriceRange(priceId: String, currency: Currency, days: String): List<HistoricalCoinRate> {
        val response = priceApi.getLastCoinRange(priceId, currency.coingeckoId, days)

        return response.prices.map { (timestampRaw, rateRaw) ->
            HistoricalCoinRate(
                timestamp = timestampRaw.toLong().milliseconds.inWholeSeconds,
                rate = rateRaw
            )
        }
    }

    override suspend fun getCoinRates(priceIds: Set<String>, currency: Currency): Map<String, CoinRateChange?> {
        val sortedPriceIds = priceIds.toList().sorted()
        return apiCall { priceApi.getAssetPrice(sortedPriceIds.asQueryParam(), currency = currency.coingeckoId, includeRateChange = true) }
            .mapValues {
                val price = it.value[currency.coingeckoId].orZero()
                val recentRate = it.value[PriceApi.getRecentRateFieldName(currency.coingeckoId)].orZero()
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
