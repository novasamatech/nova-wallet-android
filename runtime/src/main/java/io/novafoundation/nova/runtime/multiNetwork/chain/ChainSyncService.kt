package io.novafoundation.nova.runtime.multiNetwork.chain

import com.google.gson.Gson
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.common.utils.retryUntilDone
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.core_db.dao.FullAssetIdLocal
import io.novafoundation.nova.core_db.ext.fullId
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal.Companion.ENABLED_DEFAULT_BOOL
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.ChainNodeLocal
import io.novafoundation.nova.core_db.model.chain.NodeSelectionPreferencesLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapExternalApisToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapRemoteAssetToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapRemoteChainToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapRemoteExplorersToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapRemoteNodesToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.ChainFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChainSyncService(
    private val chainDao: ChainDao,
    private val chainFetcher: ChainFetcher,
    private val gson: Gson
) {

    suspend fun syncUp() = withContext(Dispatchers.Default) {
        val localChainsJoinedInfo = chainDao.getJoinChainInfo()
        val oldChains = localChainsJoinedInfo.map { it.chain }.filter { it.source != ChainLocal.Source.CUSTOM }
        val oldAssets = localChainsJoinedInfo.flatMap { it.assets }.filter { it.source == AssetSourceLocal.DEFAULT }
        val oldNodes = localChainsJoinedInfo.flatMap { it.nodes }.filter { it.source != ChainNodeLocal.Source.CUSTOM }
        val oldExplorers = localChainsJoinedInfo.flatMap { it.explorers }
        val oldExternalApis = localChainsJoinedInfo.flatMap { it.externalApis }
        val oldNodeSelectionPreferences = localChainsJoinedInfo.mapNotNull { it.nodeSelectionPreferences }

        val oldChainsById = oldChains.associateBy { it.id }
        val associatedOldAssets = oldAssets.associateBy { it.fullId() }

        val remoteChains = retryUntilDone { chainFetcher.getChains() }

        val newChains = remoteChains.map { mapRemoteChainToLocal(it, oldChainsById[it.chainId], source = ChainLocal.Source.DEFAULT, gson) }
        val newAssets = remoteChains.flatMap { chain ->
            chain.assets.map {
                val fullAssetId = FullAssetIdLocal(chain.chainId, it.assetId)
                val oldAsset = associatedOldAssets[fullAssetId]
                mapRemoteAssetToLocal(chain, it, gson, oldAsset?.enabled ?: ENABLED_DEFAULT_BOOL)
            }
        }
        val newNodes = remoteChains.flatMap(::mapRemoteNodesToLocal)
        val newExplorers = remoteChains.flatMap(::mapRemoteExplorersToLocal)
        val newExternalApis = remoteChains.flatMap(::mapExternalApisToLocal)
        val newNodeSelectionPreferences = nodeSelectionPreferencesFor(newChains, oldNodeSelectionPreferences)

        val chainsDiff = CollectionDiffer.findDiff(newChains, oldChains, forceUseNewItems = false)
        val assetDiff = CollectionDiffer.findDiff(newAssets, oldAssets, forceUseNewItems = false)
        val nodesDiff = CollectionDiffer.findDiff(newNodes, oldNodes, forceUseNewItems = false)
        val explorersDiff = CollectionDiffer.findDiff(newExplorers, oldExplorers, forceUseNewItems = false)
        val externalApisDiff = CollectionDiffer.findDiff(newExternalApis, oldExternalApis, forceUseNewItems = false)
        val nodeSelectionPreferencesDiff = CollectionDiffer.findDiff(oldNodeSelectionPreferences, newNodeSelectionPreferences, forceUseNewItems = false)

        chainDao.applyDiff(
            chainDiff = chainsDiff,
            assetsDiff = assetDiff,
            nodesDiff = nodesDiff,
            explorersDiff = explorersDiff,
            externalApisDiff = externalApisDiff,
            nodeSelectionPreferencesDiff = nodeSelectionPreferencesDiff
        )
    }

    private fun nodeSelectionPreferencesFor(
        chains: List<ChainLocal>,
        oldNodeSelectionPreferences: List<NodeSelectionPreferencesLocal>
    ): List<NodeSelectionPreferencesLocal> {
        val preferencesById = oldNodeSelectionPreferences.associateBy { it.chainId }
        return chains.map {
            preferencesById[it.id]
                ?: NodeSelectionPreferencesLocal(
                    chainId = it.id,
                    autoBalanceEnabled = NodeSelectionPreferencesLocal.DEFAULT_AUTO_BALANCE_BOOLEAN,
                    selectedNodeUrl = null
                )
        }
    }
}
