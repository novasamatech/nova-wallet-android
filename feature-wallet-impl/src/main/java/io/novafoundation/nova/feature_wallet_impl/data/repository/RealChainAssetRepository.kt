package io.novafoundation.nova.feature_wallet_impl.data.repository

import com.google.gson.Gson
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainAssetLocalToAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainAssetToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.toPair

class RealChainAssetRepository(
    private val chainAssetDao: ChainAssetDao,
    private val gson: Gson
) : ChainAssetRepository {
    override suspend fun setAssetsEnabled(enabled: Boolean, assetIds: List<FullChainAssetId>) {
        val localAssetIds = assetIds.map { it.toPair() }

        chainAssetDao.setAssetsEnabled(enabled, localAssetIds)
    }

    override suspend fun insertCustomAsset(chainAsset: Chain.Asset) {
        val localAsset = mapChainAssetToLocal(chainAsset, gson)
        chainAssetDao.insertAsset(localAsset)
    }

    override suspend fun getEnabledAssets(): List<Chain.Asset> {
        return chainAssetDao.getEnabledAssets().map { mapChainAssetLocalToAsset(it, gson) }
    }
}
