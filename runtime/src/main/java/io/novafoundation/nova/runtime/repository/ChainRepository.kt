package io.novafoundation.nova.runtime.repository

import com.google.gson.Gson
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainAssetToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainExplorerToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainNodeToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapNodeSelectionPreferencesToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface ChainRepository {

    suspend fun addChain(chain: Chain)
}

class RealChainRepository(
    private val chainDao: ChainDao,
    private val gson: Gson
) : ChainRepository {

    override suspend fun addChain(chain: Chain) {
        chainDao.addChain(
            chain = mapChainToLocal(chain, gson),
            assets = chain.assets.map { mapChainAssetToLocal(it, gson) },
            nodes = chain.nodes.nodes.map { mapChainNodeToLocal(it) },
            explorers = chain.explorers.map { mapChainExplorerToLocal(it) },
            externalApis = emptyList(), // TODO Mapping is quite difficult to do it now (We don't have flows to add external apis in this time)
            nodeSelectionPreferences = mapNodeSelectionPreferencesToLocal(chain)
        )
    }
}
