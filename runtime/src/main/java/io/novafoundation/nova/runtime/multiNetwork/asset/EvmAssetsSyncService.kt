package io.novafoundation.nova.runtime.multiNetwork.asset

import com.google.gson.Gson
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.common.utils.retryUntilDone
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.core_db.ext.fullId
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal
import io.novafoundation.nova.runtime.multiNetwork.asset.remote.AssetFetcher
import io.novafoundation.nova.runtime.multiNetwork.chain.DefaultAssetsRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapEVMAssetRemoteToLocalAssets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EvmAssetsSyncService(
    private val chainDao: ChainDao,
    private val chainAssetDao: ChainAssetDao,
    private val chainFetcher: AssetFetcher,
    private val defaultAssetsRepository: DefaultAssetsRepository,
    private val gson: Gson,
) {

    suspend fun syncUp() = withContext(Dispatchers.Default) {
        syncEVMAssets()
    }

    private suspend fun syncEVMAssets() {
        val availableChainIds = chainDao.getAllChainIds().toSet()

        val oldAssets = chainAssetDao.getAssetsBySource(AssetSourceLocal.ERC20)
        val associatedOldAssets = oldAssets.associateBy { it.fullId() }
        val initialAssetEnabling = retryUntilDone { defaultAssetsRepository.initialAssetEnabling() }

        val newAssets = retryUntilDone { chainFetcher.getEVMAssets() }
            .flatMap { mapEVMAssetRemoteToLocalAssets(it, gson) }
            .mapNotNull { new ->
                // handle misconfiguration between chains.json and assets.json when assets contains asset for chain that is not present in chain
                if (new.chainId !in availableChainIds) return@mapNotNull null

                val old = associatedOldAssets[new.fullId()]
                new.copy(enabled = old?.enabled ?: initialAssetEnabling.isEnabled(new.fullId()))
            }

        val diff = CollectionDiffer.findDiff(newAssets, oldAssets, forceUseNewItems = false)
        chainAssetDao.updateAssets(diff)
    }
}
