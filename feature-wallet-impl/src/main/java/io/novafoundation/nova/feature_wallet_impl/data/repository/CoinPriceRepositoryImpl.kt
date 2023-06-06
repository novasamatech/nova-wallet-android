package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.source.CoinPriceDataSource
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate

class CoinPriceRepositoryImpl(
    private val coinRateDataSource: CoinPriceDataSource
) : CoinPriceRepository {
    override suspend fun getCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Long): CoinRate? {
        return coinRateDataSource.getCoinPriceAtTime(priceId, currency, timestamp)
    }

    override suspend fun getCoinPriceRange(priceId: String, currency: Currency, fromTimestamp: Long, toTimestamp: Long): List<HistoricalCoinRate> {
        return coinRateDataSource.getCoinPriceRange(priceId, currency, fromTimestamp, toTimestamp)
    }

    override suspend fun getCoinRates(priceIds: Set<String>, currency: Currency): Map<String, CoinRateChange?> {
        return coinRateDataSource.getCoinRates(priceIds, currency)
    }

    override suspend fun getCoinRate(priceId: String, currency: Currency): CoinRateChange? {
        return coinRateDataSource.getCoinRate(priceId, currency)
    }
}
