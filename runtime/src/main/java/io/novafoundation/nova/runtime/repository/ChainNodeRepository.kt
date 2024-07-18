package io.novafoundation.nova.runtime.repository

import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.core_db.model.chain.ChainNodeLocal

interface ChainNodeRepository {

    suspend fun createChainNode(chainId: String, url: String, name: String)

    suspend fun saveChainNode(chainId: String, oldUrl: String, url: String, name: String)
}

class RealChainNodeRepository(
    private val chainDao: ChainDao
) : ChainNodeRepository {

    override suspend fun createChainNode(chainId: String, url: String, name: String) {
        val lastOrderId = chainDao.getLastChainNodeOrderId(chainId)
        chainDao.addChainNode(
            ChainNodeLocal(
                chainId = chainId,
                url = url,
                name = name,
                orderId = lastOrderId + 1,
                source = ChainNodeLocal.Source.CUSTOM
            )
        )
    }

    override suspend fun saveChainNode(chainId: String, oldUrl: String, url: String, name: String) {
        chainDao.updateChainNode(chainId, oldUrl, url, name)
    }
}
