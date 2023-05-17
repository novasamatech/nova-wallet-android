package io.novafoundation.nova.feature_staking_impl.data.dashboard.cache

import io.novafoundation.nova.core_db.dao.StakingDashboardDao
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface StakingDashboardCache {

    suspend fun update(
        chainId: ChainId,
        assetId: ChainAssetId,
        stakingTypeLocal: String,
        metaAccountId: Long,
        updating: (previousValue: StakingDashboardItemLocal?) -> StakingDashboardItemLocal
    )
}

class RealStakingDashboardCache(
    private val dao: StakingDashboardDao
) : StakingDashboardCache {

    override suspend fun update(
        chainId: ChainId,
        assetId: ChainAssetId,
        stakingTypeLocal: String,
        metaAccountId: Long,
        updating: (previousValue: StakingDashboardItemLocal?) -> StakingDashboardItemLocal
    ) {
        val fromCache = dao.getDashboardItem(chainId, assetId, stakingTypeLocal, metaAccountId)
        val toInsert = updating(fromCache)

        dao.insertItem(toInsert)
    }
}
