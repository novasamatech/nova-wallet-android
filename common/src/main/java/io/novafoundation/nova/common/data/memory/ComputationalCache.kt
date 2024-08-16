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
        flowLazy: suspend () -> Flow<T>
    ): Flow<T>
}
