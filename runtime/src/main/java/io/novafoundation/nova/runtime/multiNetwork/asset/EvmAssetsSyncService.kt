package io.novafoundation.nova.runtime.multiNetwork.asset

import com.google.gson.Gson
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.common.utils.retryUntilDone
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.core_db.ext.fullId
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal.Companion.ENABLED_DEFAULT_BOOL
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
        val associatedOldAssets = oldAssets.associateBy { it.fullId() }

        val newAssets = retryUntilDone { chainFetcher.getEVMAssets() }
            .flatMap { mapEVMAssetRemoteToLocalAssets(it, gson) }
            .map { new ->
                val old = associatedOldAssets[new.fullId()]
                new.copy(enabled = old?.enabled ?: ENABLED_DEFAULT_BOOL)
            }

        val diff = CollectionDiffer.findDiff(newAssets, oldAssets, forceUseNewItems = false)
        dao.updateAssets(diff)
    }
}
