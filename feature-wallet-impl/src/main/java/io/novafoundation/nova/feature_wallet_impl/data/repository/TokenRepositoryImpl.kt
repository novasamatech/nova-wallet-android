package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.core_db.dao.TokenDao
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapTokenWithCurrencyToToken
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TokenRepositoryImpl(
    private val tokenDao: TokenDao
) : TokenRepository {

    override suspend fun observeTokens(chainAssets: List<Chain.Asset>): Flow<Map<Chain.Asset, Token>> {
        val symbols = chainAssets.map { it.symbol }.distinct()

        return tokenDao.observeTokens(symbols).map { tokens ->
            val tokensBySymbol = tokens.associateBy { it.token?.tokenSymbol }

            chainAssets.associateWith {
                mapTokenWithCurrencyToToken(tokensBySymbol.getValue(it.symbol), it)
            }
        }
    }

    override suspend fun getToken(chainAsset: Chain.Asset): Token = withContext(Dispatchers.Default) {
        val tokenLocal = tokenDao.getToken(chainAsset.symbol)!!

        mapTokenWithCurrencyToToken(tokenLocal, chainAsset)
    }

    override fun observeToken(chainAsset: Chain.Asset): Flow<Token> {
        return tokenDao.observeToken(chainAsset.symbol)
            .map {
                mapTokenWithCurrencyToToken(it, chainAsset)
            }
    }
}
