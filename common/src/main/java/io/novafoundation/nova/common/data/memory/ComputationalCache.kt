package io.novafoundation.nova.common.data.memory

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

interface ComputationalCache {

    /**
     * Caches [computation] between calls until all supplied [scope]s have been cancelled
     */
    suspend fun <T> useCache(
        key: String,
        scope: CoroutineScope,
        computation: suspend CoroutineScope.() -> T
    ): T

    fun <T> useSharedFlow(
        key: String,
        scope: CoroutineScope,
        flowLazy: suspend CoroutineScope.() -> Flow<T>
    ): Flow<T>
}

/**
 * Shares the producer by [key] while it has collectors. Each collection owns its scope, so finishing
 * the last collection releases the cached producer even if the caller's parent scope is still alive.
 */
fun <T> ComputationalCache.observeSharedFlow(
    key: String,
    flowLazy: suspend CoroutineScope.() -> Flow<T>
): Flow<T> = flow {
    coroutineScope {
        emitAll(useSharedFlow(key, this, flowLazy))
    }
}

context(ComputationalScope)
suspend fun <T> ComputationalCache.useCache(
    key: String,
    computation: suspend CoroutineScope.() -> T
): T = useCache(key, this@ComputationalScope, computation)

context(ComputationalScope)
fun <T> ComputationalCache.useSharedFlow(
    key: String,
    flowLazy: suspend CoroutineScope.() -> Flow<T>
): Flow<T> = useSharedFlow(key, this@ComputationalScope, flowLazy)
