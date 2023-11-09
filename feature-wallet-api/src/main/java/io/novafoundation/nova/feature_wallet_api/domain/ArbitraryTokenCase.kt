package io.novafoundation.nova.feature_wallet_api.domain

import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalToken
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.time.Duration

interface ArbitraryTokenUseCase {

    fun historicalTokenFlow(chainAsset: Chain.Asset, at: Duration): Flow<HistoricalToken>

    suspend fun historicalToken(chainAsset: Chain.Asset, at: Duration): HistoricalToken
}

class RealArbitraryTokenUseCase(
    private val coinPriceRepository: CoinPriceRepository,
    private val currencyRepository: CurrencyRepository,
) : ArbitraryTokenUseCase {

    override fun historicalTokenFlow(chainAsset: Chain.Asset, at: Duration): Flow<HistoricalToken> {
       return flowOf { historicalToken(chainAsset, at) }
    }

    override suspend fun historicalToken(chainAsset: Chain.Asset, at: Duration): HistoricalToken = withContext(Dispatchers.IO) {
        val currency = currencyRepository.getSelectedCurrency()
        val priceId = chainAsset.priceId

        val rate = if (priceId != null) {
            coinPriceRepository.getCoinPriceAtTime(priceId, currency, at)
        } else {
            null
        }

        HistoricalToken(currency, rate, chainAsset)
    }
}
