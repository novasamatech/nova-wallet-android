package io.novafoundation.nova.feature_wallet_impl.data.source

import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.utils.asQueryParam
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.second
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.network.coingecko.CoingeckoApi
import io.novafoundation.nova.feature_wallet_api.data.source.CoinPriceRemoteDataSource
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import kotlin.time.Duration.Companion.milliseconds

class CoingeckoCoinPriceDataSource(
    private val coingeckoApi: CoingeckoApi,
    private val httpExceptionHandler: HttpExceptionHandler
) : CoinPriceRemoteDataSource {

    override suspend fun getCoinPriceRange(priceId: String, currency: Currency, fromTimestamp: Long, toTimestamp: Long): List<HistoricalCoinRate> {
        val response = coingeckoApi.getCoinRange(priceId, currency.coingeckoId, fromTimestamp, toTimestamp)
        return response.prices.map {
            HistoricalCoinRate(
                it.first().toLong().milliseconds.inWholeSeconds,
                it.second()
            )
        }
    }

    override suspend fun getCoinRates(priceIds: Set<String>, currency: Currency): Map<String, CoinRateChange?> {
        return apiCall { coingeckoApi.getAssetPrice(priceIds.asQueryParam(), currency = currency.coingeckoId, includeRateChange = true) }
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
}
