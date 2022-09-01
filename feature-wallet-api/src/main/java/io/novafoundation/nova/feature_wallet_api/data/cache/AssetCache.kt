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

    /**
     * @return true if asset was changed. false if it remained the same
     */
    suspend fun updateAsset(
        metaId: Long,
        chainAsset: Chain.Asset,
        builder: (local: AssetLocal) -> AssetLocal,
    ): Boolean = withContext(Dispatchers.IO) {
        val assetId = chainAsset.id
        val chainId = chainAsset.chainId

        assetUpdateMutex.withLock {
            val cachedAsset = assetDao.getAsset(metaId, chainId, assetId) ?: AssetLocal.createEmpty(assetId, chainId, metaId)

            val newAsset = builder.invoke(cachedAsset)

            assetDao.insertAsset(newAsset)

            cachedAsset != newAsset
        }
    }

    /**
     * @see updateAsset
     */
    suspend fun updateAsset(
        accountId: AccountId,
        chainAsset: Chain.Asset,
        builder: (local: AssetLocal) -> AssetLocal,
    ): Boolean = withContext(Dispatchers.IO) {
        val applicableMetaAccount = accountRepository.findMetaAccount(accountId)

        applicableMetaAccount?.let {
            updateAsset(it.id, chainAsset, builder)
        } ?: false
    }

    suspend fun insertTokens(tokens: List<TokenLocal>) = tokenDao.insertTokens(tokens)
}
