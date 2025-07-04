package io.novafoundation.nova.feature_wallet_api.domain

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalToken
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration

interface ArbitraryTokenUseCase {

    fun historicalTokenFlow(chainAsset: Chain.Asset, at: Duration): Flow<HistoricalToken>

    suspend fun historicalToken(chainAsset: Chain.Asset, at: Duration): HistoricalToken

    suspend fun getToken(chainAssetId: FullChainAssetId): Token
}

@FeatureScope
class RealArbitraryTokenUseCase @Inject constructor(
    private val coinPriceRepository: CoinPriceRepository,
    private val currencyRepository: CurrencyRepository,
    private val tokenRepository: TokenRepository,
    private val chainRegistry: ChainRegistry,
) : ArbitraryTokenUseCase {

    override fun historicalTokenFlow(chainAsset: Chain.Asset, at: Duration): Flow<HistoricalToken> {
        return flowOf { historicalToken(chainAsset, at) }
    }

    override suspend fun historicalToken(chainAsset: Chain.Asset, at: Duration): HistoricalToken = withContext(Dispatchers.IO) {
        val currency = currencyRepository.getSelectedCurrency()
        val priceId = chainAsset.priceId

        val rate = if (priceId != null) {
            runCatching { coinPriceRepository.getCoinPriceAtTime(priceId, currency, at) }
                .getOrNull()
        } else {
            null
        }

        HistoricalToken(currency, rate, chainAsset)
    }

    override suspend fun getToken(chainAssetId: FullChainAssetId): Token {
        return tokenRepository.getToken(chainRegistry.asset(chainAssetId))
    }
}
