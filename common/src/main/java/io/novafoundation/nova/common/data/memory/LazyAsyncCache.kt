package io.novafoundation.nova.common.data.memory

import io.novafoundation.nova.common.utils.invokeOnCompletion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface LazyAsyncCache<K, V> {

    suspend fun getOrCompute(key: K): V
}

/**
 * In-memory cache primitive that caches asynchronously computed value
 * Lifetime of the cache itself is determine by supplied [CoroutineScope]
 */
fun <K, V> LazyAsyncCache(coroutineScope: CoroutineScope, compute: AsyncCacheCompute<K, V>): LazyAsyncCache<K, V> {
    return RealLazyAsyncCache(coroutineScope, compute)
}

/**
 * Specialization of [LazyAsyncCache] that's cached value is a [SharedFlow] shared in the supplied [coroutineScope]
 */
inline fun <K, V> SharedFlowCache(
    coroutineScope: CoroutineScope,
    crossinline compute: suspend (key: K) -> Flow<V>
): LazyAsyncCache<K, SharedFlow<V>> {
    return LazyAsyncCache(coroutineScope) { key ->
        compute(key).shareIn(coroutineScope, SharingStarted.Eagerly, replay = 1)
    }
}

typealias AsyncCacheCompute<K, V> = suspend (key: K) -> V

private class RealLazyAsyncCache<K, V>(
    private val lifetime: CoroutineScope,
    private val compute: AsyncCacheCompute<K, V>,
) : LazyAsyncCache<K, V> {

    private val mutex = Mutex()
    private val cache = mutableMapOf<K, V>()

    override suspend fun getOrCompute(key: K): V {
        mutex.withLock {
            if (key in cache) return cache.getValue(key)

            return compute(key).also {
                cache[key] = it
            }
        }
    }

    init {
        lifetime.invokeOnCompletion {
            clearCache()
        }
    }

    // GlobalScope job is fine here since it just for clearing the map
    @OptIn(DelicateCoroutinesApi::class)
    private fun clearCache() = GlobalScope.launch {
        mutex.withLock { cache.clear() }
    }
}
