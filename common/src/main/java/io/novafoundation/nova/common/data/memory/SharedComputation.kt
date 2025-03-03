package io.novafoundation.nova.common.data.memory

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

abstract class SharedComputation(
    private val computationalCache: ComputationalCache
) {

    context(ComputationalScope)
    protected fun <T> cachedFlow(
        vararg keyArgs: String,
        flowLazy: suspend () -> Flow<T>
    ): Flow<T> {
        val key = keyArgs.joinToString(separator = ".")

        return computationalCache.useSharedFlow(key, flowLazy)
    }

    context(ComputationalScope)
    protected suspend fun <T> cachedValue(
        vararg keyArgs: String,
        valueLazy: suspend CoroutineScope.() -> T
    ): T {
        val key = keyArgs.joinToString(separator = ".")

        return computationalCache.useCache(key, valueLazy)
    }
}
