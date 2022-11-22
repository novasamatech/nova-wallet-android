package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.toPair

class RealChainAssetRepository(
    private val chainAssetDao: ChainAssetDao
) : ChainAssetRepository {
    override suspend fun setAssetsEnabled(enabled: Boolean, assetIds: List<FullChainAssetId>) {
        val localAssetIds = assetIds.map { it.toPair() }

        chainAssetDao.setAssetsEnabled(enabled, localAssetIds)
    }
}
