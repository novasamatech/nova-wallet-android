package io.novafoundation.nova.feature_wallet_api.data.cache

import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.core_db.dao.AssetDao
import io.novafoundation.nova.core_db.dao.AssetReadOnlyCache
import io.novafoundation.nova.core_db.dao.ClearAssetsParams
import io.novafoundation.nova.core_db.dao.TokenDao
import io.novafoundation.nova.core_db.model.AssetLocal
import io.novafoundation.nova.core_db.model.TokenLocal
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.ext.enabledAssets
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
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
        val applicableMetaAccount = accountRepository.findMetaAccount(accountId, chainAsset.chainId)

        applicableMetaAccount?.let {
            updateAsset(it.id, chainAsset, builder)
        } ?: false
    }

    suspend fun updateAssetsByChain(
        metaAccount: MetaAccount,
        chain: Chain,
        builder: (Chain.Asset) -> AssetLocal
    ): CollectionDiffer.Diff<AssetLocal> = withContext(Dispatchers.IO) {
        val oldAssetsLocal = getAssetsInChain(metaAccount.id, chain.id)
        val newAssetsLocal = chain.enabledAssets().map { builder(it) }
        val diff = CollectionDiffer.findDiff(newAssetsLocal, oldAssetsLocal, forceUseNewItems = false)
        assetDao.insertAssets(diff.newOrUpdated)
        diff
    }

    suspend fun clearAssets(assetIds: List<FullChainAssetId>) = withContext(Dispatchers.IO) {
        val localAssetIds = assetIds.map { ClearAssetsParams(it.chainId, it.assetId) }
        assetDao.clearAssets(localAssetIds)
    }

    suspend fun deleteAllTokens() {
        tokenDao.deleteAll()
    }

    suspend fun updateTokens(newTokens: List<TokenLocal>) {
        val oldTokens = tokenDao.getTokens()
        val diff = CollectionDiffer.findDiff(newTokens, oldTokens, forceUseNewItems = false)
        tokenDao.applyDiff(diff)
    }

    suspend fun insertToken(tokens: TokenLocal) = tokenDao.insertToken(tokens)
}
