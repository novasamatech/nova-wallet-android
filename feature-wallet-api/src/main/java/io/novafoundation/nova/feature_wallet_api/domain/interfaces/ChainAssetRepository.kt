package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.toPair

interface ChainAssetRepository {

    suspend fun setAssetsEnabled(enabled: Boolean, assetIds: List<FullChainAssetId>)
}

class RealChainAssetRepository(
    private val chainAssetDao: ChainAssetDao
): ChainAssetRepository {
    override suspend fun setAssetsEnabled(enabled: Boolean, assetIds: List<FullChainAssetId>) {
        val localAssetIds = assetIds.map { it.toPair() }

        chainAssetDao.setAssetsEnabled(enabled, localAssetIds)
    }
}
