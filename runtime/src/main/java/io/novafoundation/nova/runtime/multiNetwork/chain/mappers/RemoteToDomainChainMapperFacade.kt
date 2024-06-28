package io.novafoundation.nova.runtime.multiNetwork.chain.mappers

import com.google.gson.Gson
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.NodeSelectionPreferencesLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainRemote

class RemoteToDomainChainMapperFacade(
    private val gson: Gson
) {

    fun mapRemoteChainToDomain(chainRemote: ChainRemote, source: Chain.Source): Chain {
        val localSource = when (source) {
            Chain.Source.DEFAULT -> ChainLocal.Source.DEFAULT
            Chain.Source.CUSTOM -> ChainLocal.Source.CUSTOM
        }
        val chainLocal = mapRemoteChainToLocal(chainRemote, null, localSource, gson)
        val assetsLocal = chainRemote.assets.map { mapRemoteAssetToLocal(chainRemote, it, gson, isEnabled = true) }
        val nodesLocal = mapRemoteNodesToLocal(chainRemote)
        val explorersLocal = mapRemoteExplorersToLocal(chainRemote)
        val externalApisLocal = mapExternalApisToLocal(chainRemote)

        return mapChainLocalToChain(
            chainLocal = chainLocal,
            nodesLocal = nodesLocal,
            nodeSelectionPreferences = NodeSelectionPreferencesLocal(chainLocal.id, autoBalanceEnabled = true, selectedNodeUrl = null),
            assetsLocal = assetsLocal,
            explorersLocal = explorersLocal,
            externalApisLocal = externalApisLocal,
            gson = gson
        )
    }
}
