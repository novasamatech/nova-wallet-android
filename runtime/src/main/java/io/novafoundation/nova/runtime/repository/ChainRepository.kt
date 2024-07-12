package io.novafoundation.nova.runtime.repository

import com.google.gson.Gson
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainAssetToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainExplorerToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainNodeToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapNodeSelectionPreferencesToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface ChainRepository {

    suspend fun addChain(chain: Chain)

    suspend fun editChain(
        chainId: String,
        chainName: String,
        tokenSymbol: String,
        blockExplorer: Chain.Explorer?,
        priceId: String?
    )

    suspend fun deleteNetwork(chainId: String)

    suspend fun deleteNode(chainId: String, nodeUrl: String)
}

class RealChainRepository(
    private val chainRegistry: ChainRegistry,
    private val chainDao: ChainDao,
    private val gson: Gson
) : ChainRepository {

    override suspend fun addChain(chain: Chain) {
        chainDao.addChainOrUpdate(
            chain = mapChainToLocal(chain, gson),
            assets = chain.assets.map { mapChainAssetToLocal(it, gson) },
            nodes = chain.nodes.nodes.map { mapChainNodeToLocal(it) },
            explorers = chain.explorers.map { mapChainExplorerToLocal(it) },
            externalApis = emptyList(), // TODO Mapping is quite difficult to do it now (We don't have flows to add external apis in this time)
            nodeSelectionPreferences = mapNodeSelectionPreferencesToLocal(chain)
        )
    }

    override suspend fun editChain(chainId: String, chainName: String, tokenSymbol: String, blockExplorer: Chain.Explorer?, priceId: String?) {
        val chain = chainRegistry.getChain(chainId)
        chainDao.editChain(
            chainId,
            chain.utilityAsset.id,
            chainName,
            tokenSymbol,
            blockExplorer?.let { mapChainExplorerToLocal(it) },
            priceId
        )
    }

    override suspend fun deleteNetwork(chainId: String) {
        chainDao.deleteChain(chainId)
    }

    override suspend fun deleteNode(chainId: String, nodeUrl: String) {
        chainDao.deleteNode(chainId, nodeUrl)
    }
}
