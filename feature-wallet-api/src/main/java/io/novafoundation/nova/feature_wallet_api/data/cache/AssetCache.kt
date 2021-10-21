package io.novafoundation.nova.feature_wallet_api.data.cache

import io.novafoundation.nova.core_db.dao.AssetDao
import io.novafoundation.nova.core_db.dao.AssetReadOnlyCache
import io.novafoundation.nova.core_db.dao.TokenDao
import io.novafoundation.nova.core_db.model.AssetLocal
import io.novafoundation.nova.core_db.model.TokenLocal
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AssetCache(
    private val tokenDao: TokenDao,
    private val accountRepository: AccountRepository,
    private val assetDao: AssetDao,
) : AssetReadOnlyCache by assetDao {

    private val assetUpdateMutex = Mutex()

    suspend fun updateAsset(
        metaId: Long,
        chainAsset: Chain.Asset,
        builder: (local: AssetLocal) -> AssetLocal,
    ) = withContext(Dispatchers.IO) {
        val symbol = chainAsset.symbol
        val chainId = chainAsset.chainId

        assetUpdateMutex.withLock {
            tokenDao.ensureToken(symbol)

            val cachedAsset = assetDao.getAsset(metaId, chainId, symbol)?.asset ?: AssetLocal.createEmpty(symbol, chainId, metaId)

            val newAsset = builder.invoke(cachedAsset)

            assetDao.insertAsset(newAsset)
        }
    }

    suspend fun updateAsset(
        accountId: AccountId,
        chainAsset: Chain.Asset,
        builder: (local: AssetLocal) -> AssetLocal,
    ) = withContext(Dispatchers.IO) {
        val applicableMetaAccount = accountRepository.findMetaAccount(accountId)

        applicableMetaAccount?.let {
            updateAsset(it.id, chainAsset, builder)
        }
    }

    suspend fun updateToken(
        symbol: String,
        builder: (local: TokenLocal) -> TokenLocal,
    ) = withContext(Dispatchers.IO) {
        assetUpdateMutex.withLock {
            val tokenLocal = tokenDao.getToken(symbol) ?: TokenLocal.createEmpty(symbol)

            val newToken = builder.invoke(tokenLocal)

            tokenDao.insertToken(newToken)
        }
    }

    suspend fun insertTokens(tokens: List<TokenLocal>) = tokenDao.insertTokens(tokens)
}
