package io.novafoundation.nova.runtime.multiNetwork.asset

import com.google.gson.Gson
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.common.utils.retryUntilDone
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.core_db.ext.isSameAsset
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
        val oldAssets = dao.getAssetsBySource(AssetSourceLocal.ERC20)
        val newAssets = retryUntilDone { chainFetcher.getEVMAssets() }
            .flatMap { mapEVMAssetRemoteToLocalAssets(it, gson) }
            .map { new ->
                val old = oldAssets.firstOrNull { old -> old.isSameAsset(new) }
                new.copy(enabled = old?.enabled ?: true)
            }
        val diff = CollectionDiffer.findDiff(newAssets, oldAssets, forceUseNewItems = false)
        dao.updateAssets(diff)
    }
}
