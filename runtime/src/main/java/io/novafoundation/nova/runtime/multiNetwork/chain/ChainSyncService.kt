package io.novafoundation.nova.runtime.multiNetwork.chain

import com.google.gson.Gson
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.common.utils.retryUntilDone
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.ChainFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChainSyncService(
    private val dao: ChainDao,
    private val chainFetcher: ChainFetcher,
    private val gson: Gson
) {

    suspend fun syncUp() = withContext(Dispatchers.Default) {
        val localChainsJoinedInfo = dao.getJoinChainInfo()

        val remoteChains = retryUntilDone { chainFetcher.getChains() }.map(::mapChainRemoteToChain)
        val localChains = localChainsJoinedInfo.map { mapChainLocalToChain(it, gson) }

        val diff = CollectionDiffer.findDiff(newItems = remoteChains, oldItems = localChains)

        dao.update(
            newOrUpdated = diff.newOrUpdated.map { mapChainToChainLocal(it, gson) },
            removed = diff.removed.map { mapChainToChainLocal(it, gson).chain }
        )
    }
}
