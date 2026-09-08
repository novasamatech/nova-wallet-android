package io.novafoundation.nova.feature_wallet_impl.data.repository

import com.google.gson.Gson
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.core_db.dao.SetAssetEnabledParams
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.AssetEnabledUpdate
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainAssetLocalToAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainAssetToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class RealChainAssetRepository(
    private val chainAssetDao: ChainAssetDao,
    private val gson: Gson
) : ChainAssetRepository {

    override suspend fun setAssetsEnabled(updates: List<AssetEnabledUpdate>) {
        val updateParams = updates.map { SetAssetEnabledParams(it.enabled, it.overriddenByUser, it.assetId.chainId, it.assetId.assetId) }

        chainAssetDao.setAssetsEnabled(updateParams)
    }

    override suspend fun insertCustomAsset(chainAsset: Chain.Asset) {
        val localAsset = mapChainAssetToLocal(chainAsset, gson)
        chainAssetDao.insertAsset(localAsset)
    }

    override suspend fun getEnabledAssets(): List<Chain.Asset> {
        return chainAssetDao.getEnabledAssets().map { mapChainAssetLocalToAsset(it, gson) }
    }
}
