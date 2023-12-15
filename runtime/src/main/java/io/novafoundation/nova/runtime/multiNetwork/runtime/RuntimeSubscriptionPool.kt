package io.novafoundation.nova.runtime.multiNetwork.runtime

import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import kotlinx.coroutines.cancel
import java.util.concurrent.ConcurrentHashMap

class RuntimeSubscriptionPool(
    private val chainDao: ChainDao,
    private val runtimeSyncService: RuntimeSyncService
) {

    private val pool = ConcurrentHashMap<String, RuntimeVersionSubscription>()

    fun setupRuntimeSubscription(chain: Chain, connection: ChainConnection): RuntimeVersionSubscription {
        return pool.getOrPut(chain.id) {
            RuntimeVersionSubscription(chain.id, connection, chainDao, runtimeSyncService)
        }
    }

    fun removeSubscription(chainId: String) {
        pool.remove(chainId)?.apply { cancel() }
    }
}
