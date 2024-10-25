package io.novafoundation.nova.feature_wallet_impl.data.repository

import com.google.gson.Gson
import io.novafoundation.nova.common.data.repository.AssetsIconModeService
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.core_db.dao.SetAssetEnabledParams
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainAssetLocalToAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainAssetToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

class RealChainAssetRepository(
    private val chainAssetDao: ChainAssetDao,
    private val gson: Gson,
    private val assetsIconModeService: AssetsIconModeService
) : ChainAssetRepository {

    override suspend fun setAssetsEnabled(enabled: Boolean, assetIds: List<FullChainAssetId>) {
        val updateParams = assetIds.map { SetAssetEnabledParams(enabled, it.chainId, it.assetId) }

        chainAssetDao.setAssetsEnabled(updateParams)
    }

    override suspend fun insertCustomAsset(chainAsset: Chain.Asset) {
        val localAsset = mapChainAssetToLocal(chainAsset, gson)
        chainAssetDao.insertAsset(localAsset)
    }

    override suspend fun getEnabledAssets(): List<Chain.Asset> {
        val iconMode = assetsIconModeService.getIconMode()
        return chainAssetDao.getEnabledAssets().map { mapChainAssetLocalToAsset(it, gson, iconMode) }
    }
}
