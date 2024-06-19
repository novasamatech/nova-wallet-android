package io.novafoundation.nova.runtime.multiNetwork.runtime

import io.novafoundation.nova.common.utils.newLimitedThreadPoolExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

interface ChainSyncDispatcher {

    fun isSyncing(chainId: String): Boolean

    fun syncFinished(chainId: String)

    fun cancelExistingSync(chainId: String)

    fun launchSync(chainId: String, action: suspend () -> Unit)
}

class AsyncChainSyncDispatcher(maxConcurrentUpdates: Int = 8) : ChainSyncDispatcher, CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val syncDispatcher = newLimitedThreadPoolExecutor(maxConcurrentUpdates).asCoroutineDispatcher()
    private val syncingChains = ConcurrentHashMap<String, Job>()

    override fun isSyncing(chainId: String): Boolean {
        return syncingChains.contains(chainId)
    }

    override fun syncFinished(chainId: String) {
        syncingChains.remove(chainId)
    }

    override fun cancelExistingSync(chainId: String) {
        syncingChains.remove(chainId)?.apply { cancel() }
    }

    override fun launchSync(chainId: String, action: suspend () -> Unit) {
        syncingChains[chainId] = launch(syncDispatcher) {
            action()
        }
    }
}
