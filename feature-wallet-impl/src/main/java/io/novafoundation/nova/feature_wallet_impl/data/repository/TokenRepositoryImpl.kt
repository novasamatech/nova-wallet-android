package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.core_db.dao.TokenDao
import io.novafoundation.nova.core_db.model.TokenLocal
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapTokenLocalToToken
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TokenRepositoryImpl(
    private val tokenDao: TokenDao
) : TokenRepository {

    override suspend fun getToken(chainAsset: Chain.Asset): Token = withContext(Dispatchers.Default) {
        val tokenLocal = tokenDao.getToken(chainAsset.symbol) ?: TokenLocal.createEmpty(chainAsset.symbol)

        mapTokenLocalToToken(tokenLocal, chainAsset)
    }

    override fun observeToken(chainAsset: Chain.Asset): Flow<Token> {
        return tokenDao.observeToken(chainAsset.symbol)
            .map {
                mapTokenLocalToToken(it, chainAsset)
            }
    }
}
