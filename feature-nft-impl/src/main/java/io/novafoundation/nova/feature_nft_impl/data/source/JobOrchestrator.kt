package io.novafoundation.nova.feature_nft_impl.data.source

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

class JobOrchestrator {

    private val runningJobs: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())

    private val mutex = Mutex()

    suspend fun runUniqueJob(id: String, action: suspend () -> Unit) = mutex.withLock {
        if (id in runningJobs) {
            return@withLock
        }

        runningJobs += id

        CoroutineScope(coroutineContext).async { action() }
            .invokeOnCompletion { runningJobs -= id }
    }
}
