package io.novafoundation.nova.feature_wallet_api.data.cache

import io.novafoundation.nova.core_db.dao.CoinPriceDao
import io.novafoundation.nova.core_db.model.CoinPriceLocal
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.source.CoinPriceLocalDataSource
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate

class CoinPriceLocalDataSourceImpl(
    private val coinPriceDao: CoinPriceDao
) : CoinPriceLocalDataSource {

    override suspend fun getFloorCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Long): CoinRate? {
        val coinPriceLocal = coinPriceDao.getFloorCoinPriceAtTime(priceId, currency.code, timestamp)
        return coinPriceLocal?.let { mapCoinPriceFromLocal(it) }
    }

    override suspend fun hasCeilingCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Long): Boolean {
        return coinPriceDao.hasCeilingCoinPriceAtTime(priceId, currency.code, timestamp)
    }

    override suspend fun getCoinPriceRange(priceId: String, currency: Currency, fromTimestamp: Long, toTimestamp: Long): List<HistoricalCoinRate> {
        return coinPriceDao.getCoinPriceRange(priceId, currency.code, fromTimestamp, toTimestamp)
            .map { mapCoinPriceFromLocal(it) }
    }

    override suspend fun updateCoinPrice(priceId: String, currency: Currency, coinRate: List<HistoricalCoinRate>) {
        coinPriceDao.updateCoinPrices(priceId, currency.code, coinRate.map { mapCoinPriceToLocal(priceId, currency, it) })
    }

    private fun mapCoinPriceFromLocal(coinPriceLocal: CoinPriceLocal): HistoricalCoinRate {
        return HistoricalCoinRate(
            timestamp = coinPriceLocal.timestamp,
            rate = coinPriceLocal.rate
        )
    }

    private fun mapCoinPriceToLocal(priceId: String, currency: Currency, coinPrice: HistoricalCoinRate): CoinPriceLocal {
        return CoinPriceLocal(
            priceId = priceId,
            currencyId = currency.code,
            timestamp = coinPrice.timestamp,
            rate = coinPrice.rate
        )
    }
}
