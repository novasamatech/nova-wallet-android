package io.novafoundation.nova.common.data.memory

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

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
