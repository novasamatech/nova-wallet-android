package io.novafoundation.nova.common.data.memory

import android.util.Log
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
import java.util.Collections

interface LazyAsyncMultiCache<K, V> {

    suspend fun getOrCompute(keys: List<K>): Map<K, V>
}

/**
 * In-memory cache primitive that caches asynchronously computed values
 * This is a generalization of [LazyAsyncCache] that can request batch of elements at the same time
 * Lifetime of the cache itself is determine by supplied [CoroutineScope]
 */
fun <K, V> LazyAsyncMultiCache(
    coroutineScope: CoroutineScope,
    debugLabel: String = "LazyAsyncMultiCache",
    compute: AsyncMultiCacheCompute<K, V>
): LazyAsyncMultiCache<K, V> {
    return RealLazyAsyncMultiCache(coroutineScope, debugLabel, compute)
}

typealias AsyncMultiCacheCompute<K, V> = suspend (keys: List<K>) -> Map<K, V>

private class RealLazyAsyncMultiCache<K, V>(
    lifetime: CoroutineScope,
    private val debugLabel: String,
    private val compute: AsyncMultiCacheCompute<K, V>,
) : LazyAsyncMultiCache<K, V> {

    private val mutex = Mutex()
    private val cache = mutableMapOf<K, V>()

    override suspend fun getOrCompute(keys: List<K>): Map<K, V> {
        mutex.withLock {
            Log.d(debugLabel, "Requested to fetch ${keys.size} keys")

            val missingKeys = keys - cache.keys

            if (missingKeys.isNotEmpty()) {
                Log.d(debugLabel, "Missing ${keys.size} keys")

                val newKeys = compute(missingKeys)
                require(newKeys.size == missingKeys.size) {
                    "compute() returned less keys than was requested. Make sure you return values for all requested keys"
                }
                cache.putAll(newKeys)
            } else {
                Log.d(debugLabel, "All keys are already in cache")
            }

            // Return the view of the whole cache to avoid extra allocations of the map
            return Collections.unmodifiableMap(cache)
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
