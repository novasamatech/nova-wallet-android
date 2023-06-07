package io.novafoundation.nova.feature_wallet_api.data.source

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface CoinPriceRemoteDataSource {

    suspend fun getCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Long): CoinRate?

    suspend fun getCoinPriceRange(priceId: String, currency: Currency, fromTimestamp: Long, toTimestamp: Long): List<HistoricalCoinRate>

    suspend fun getCoinRates(priceIds: Set<String>, currency: Currency): Map<String, CoinRateChange?>

    suspend fun getCoinRate(priceId: String, currency: Currency): CoinRateChange?
}

suspend fun CoinPriceRemoteDataSource.getCoinRateByAsset(asset: Chain.Asset, currency: Currency): CoinRateChange? {
    return asset.priceId?.let { priceId -> getCoinRate(priceId, currency) }
}
