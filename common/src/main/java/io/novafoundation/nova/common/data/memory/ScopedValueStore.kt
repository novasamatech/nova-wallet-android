package io.novafoundation.nova.common.data.memory

import io.novafoundation.nova.common.di.scope.ApplicationScope
import javax.inject.Inject

interface ScopedValueStore<T: Any> {

    interface Factory {

        fun <T: Any> create(cacheKey: String): ScopedValueStore<T>
    }

    context(ComputationalScope)
    suspend fun store(value: T)

    context(ComputationalScope)
    suspend fun get(): T?
}

context(ComputationalScope)
suspend fun  <T: Any> ScopedValueStore<T>.getOrThrow(): T {
    return requireNotNull(get()) {
        "Value was not set"
    }
}

@ApplicationScope
internal class RealScopedValueStoreFactory @Inject constructor(
    private val computationalCache: ComputationalCache,
): ScopedValueStore.Factory {

    override fun <T: Any> create(cacheKey: String): ScopedValueStore<T> {
       return RealScopedValueStore(computationalCache, cacheKey)
    }
}

internal class RealScopedValueStore<T: Any>(
    private val computationalCache: ComputationalCache,
    private val cacheKey: String,
) : ScopedValueStore<T> {

    context(ComputationalScope)
    override suspend fun store(value: T) {
       getEntry().value = value
    }

    context(ComputationalScope)
    override suspend fun get(): T? {
       return getEntry().value
    }

    context(ComputationalScope)
    private suspend fun getEntry(): CacheEntry<T> {
        return computationalCache.useCache(cacheKey) {
            CacheEntry()
        }
    }

    private class CacheEntry<T: Any> {

        var value: T? = null
    }
}
