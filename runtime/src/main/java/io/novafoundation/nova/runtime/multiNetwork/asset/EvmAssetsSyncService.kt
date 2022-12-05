package io.novafoundation.nova.runtime.multiNetwork.asset

import com.google.gson.Gson
import io.novafoundation.nova.common.utils.retryUntilDone
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal
import io.novafoundation.nova.runtime.multiNetwork.asset.remote.AssetFetcher
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapEVMAssetRemoteToLocalAssets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EvmAssetsSyncService(
    private val dao: ChainAssetDao,
    private val chainFetcher: AssetFetcher,
    private val gson: Gson,
) {

    suspend fun syncUp() = withContext(Dispatchers.Default) {
        syncEVMAssets()
    }

    private suspend fun syncEVMAssets() {
        val assets = retryUntilDone { chainFetcher.getEVMAssets() }
            .flatMap { mapEVMAssetRemoteToLocalAssets(it, gson) }
        dao.updateAssetsBySource(assets, AssetSourceLocal.ERC20)
    }
}
