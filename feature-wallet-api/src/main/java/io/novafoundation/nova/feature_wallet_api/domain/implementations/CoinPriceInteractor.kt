package io.novafoundation.nova.feature_wallet_api.domain.implementations

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate

class CoinPriceInteractor(private val coinPriceRepository: CoinPriceRepository) {

    suspend fun getCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Long): CoinRate? {
        return coinPriceRepository.getCoinPriceAtTime(priceId, currency, timestamp)
    }

    suspend fun getCoinPriceRange(priceId: String, currency: Currency, fromTimestamp: Long, toTimestamp: Long): List<HistoricalCoinRate> {
        return coinPriceRepository.getCoinPriceRange(priceId, currency, fromTimestamp, toTimestamp)
    }

    suspend fun getCoinRates(priceIds: Set<String>, currency: Currency): Map<String, CoinRateChange?> {
        return coinPriceRepository.getCoinRates(priceIds, currency)
    }

    suspend fun getCoinRate(priceId: String, currency: Currency): CoinRateChange? {
        return coinPriceRepository.getCoinRate(priceId, currency)
    }
}
