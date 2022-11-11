package io.novafoundation.nova.runtime.multiNetwork.asset

import io.novafoundation.nova.common.utils.retryUntilDone
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal
import io.novafoundation.nova.runtime.multiNetwork.asset.remote.AssetFetcher
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapEVMAssetRemoteToLocalAssets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssetSyncService(
    private val dao: ChainAssetDao,
    private val chainFetcher: AssetFetcher
) {

    suspend fun syncUp() = withContext(Dispatchers.Default) {
        syncEVMAssets()
    }

    private suspend fun syncEVMAssets() {
        val assets = retryUntilDone { chainFetcher.getEVMAssets() }
            .flatMap(::mapEVMAssetRemoteToLocalAssets)
        dao.updateAssetsByTokenType(assets, AssetSourceLocal.ERC20)
    }
}
