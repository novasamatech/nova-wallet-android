package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.core_db.dao.ChainAssetVisibilityDao
import io.novafoundation.nova.core_db.model.ChainAssetVisibilityLocal
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.AssetVisibilityRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RealAssetVisibilityRepository(
    private val dao: ChainAssetVisibilityDao
) : AssetVisibilityRepository {

    override fun visibilityFlow(metaId: Long): Flow<Map<FullChainAssetId, Boolean>> {
        return dao.observeVisibility(metaId).map { it.toDomain() }
    }

    override suspend fun getVisibility(metaId: Long): Map<FullChainAssetId, Boolean> {
        return dao.getVisibility(metaId).toDomain()
    }

    override suspend fun setVisibility(metaId: Long, choices: Map<FullChainAssetId, Boolean>) {
        if (choices.isEmpty()) return

        dao.setVisibility(choices.map { (assetId, visible) -> assetId.toLocal(metaId, visible) })
    }

    override suspend fun showIfUndecided(metaId: Long, assetIds: Collection<FullChainAssetId>) {
        if (assetIds.isEmpty()) return

        dao.insertIfAbsent(assetIds.map { it.toLocal(metaId, visible = true) })
    }

    private fun List<ChainAssetVisibilityLocal>.toDomain(): Map<FullChainAssetId, Boolean> {
        return associate { FullChainAssetId(it.chainId, it.assetId) to it.visible }
    }

    private fun FullChainAssetId.toLocal(metaId: Long, visible: Boolean): ChainAssetVisibilityLocal {
        return ChainAssetVisibilityLocal(metaId = metaId, chainId = chainId, assetId = assetId, visible = visible)
    }
}
