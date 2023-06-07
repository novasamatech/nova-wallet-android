package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history

import io.novafoundation.nova.common.utils.binarySearchFloor
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

abstract class BaseAssetHistory(internal val coinPriceRepository: CoinPriceRepository) : AssetHistory {

    protected suspend fun getCoinPriceRange(
        chainAsset: Chain.Asset,
        currency: Currency,
        from: Long,
        to: Long
    ): List<HistoricalCoinRate> {
        val offsetFrom = from.seconds
            .minus(1.hours)
            .inWholeSeconds

        val offsetTo = to.seconds
            .plus(1.hours)
            .inWholeSeconds

        return runCatching { coinPriceRepository.getCoinPriceRange(chainAsset.priceId!!, currency, offsetFrom, offsetTo) }
            .getOrNull()
            ?: emptyList()
    }

    protected fun List<HistoricalCoinRate>.findFloorRate(timestamp: Long): CoinRate? {
        if (this.isEmpty()) return null

        val id = binarySearchFloor {
            val itemTimestamp = it.timestamp.milliseconds.inWholeSeconds
            itemTimestamp.compareTo(timestamp)
        }

        return this.getOrNull(id)
    }
}
