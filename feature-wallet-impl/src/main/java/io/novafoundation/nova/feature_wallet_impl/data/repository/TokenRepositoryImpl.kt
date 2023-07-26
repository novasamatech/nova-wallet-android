package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.core_db.dao.TokenDao
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapTokenLocalToToken
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapTokenWithCurrencyToToken
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TokenRepositoryImpl(
    private val tokenDao: TokenDao
) : TokenRepository {

    override suspend fun observeTokens(chainAssets: List<Chain.Asset>): Flow<Map<FullChainAssetId, Token>> {
        if (chainAssets.isEmpty()) return flowOf(emptyMap())

        val symbols = chainAssets.map { it.symbol }.distinct()

        return tokenDao.observeTokensWithCurrency(symbols).map { tokens ->
            val tokensBySymbol = tokens.associateBy { it.token?.tokenSymbol }
            val currency = tokens.first().currency

            chainAssets.associateBy(
                keySelector = { chainAsset -> chainAsset.fullId },
                valueTransform = { chainAsset ->
                    mapTokenLocalToToken(
                        tokenLocal = tokensBySymbol[chainAsset.symbol]?.token,
                        currencyLocal = currency,
                        chainAsset = chainAsset
                    )
                }
            )
        }
    }

    override suspend fun getToken(chainAsset: Chain.Asset): Token = withContext(Dispatchers.Default) {
        val tokenLocal = tokenDao.getTokenWithCurrency(chainAsset.symbol)!!

        mapTokenWithCurrencyToToken(tokenLocal, chainAsset)
    }

    override fun observeToken(chainAsset: Chain.Asset): Flow<Token> {
        return tokenDao.observeTokenWithCurrency(chainAsset.symbol)
            .map {
                mapTokenWithCurrencyToToken(it, chainAsset)
            }
    }
}
