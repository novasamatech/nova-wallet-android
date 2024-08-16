package io.novafoundation.nova.common.data.memory

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invokeOnCompletion
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private typealias Awaitable<T> = suspend () -> T
private typealias AwaitableConstructor<T> = suspend CoroutineScope.() -> Awaitable<T>

internal class RealComputationalCache : ComputationalCache, CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private class Entry(
        val dependents: MutableSet<CoroutineScope>,
        val aggregateScope: CoroutineScope,
        val awaitable: Awaitable<Any?>
    )

    private val memory = mutableMapOf<String, Entry>()
    private val mutex = Mutex()

    override suspend fun <T> useCache(
        key: String,
        scope: CoroutineScope,
        computation: suspend CoroutineScope.() -> T
    ): T = withContext(Dispatchers.Default) {
        useCacheInternal(key, scope) {
            val deferred = async { this@useCacheInternal.computation() }

            return@useCacheInternal { deferred.await() }
        }
    }

    override fun <T> useSharedFlow(
        key: String,
        scope: CoroutineScope,
        flowLazy: suspend () -> Flow<T>
    ): Flow<T> {
        return flowOfAll {
            useCacheInternal(key, scope) {
                val inner = singleReplaySharedFlow<T>()

                launch {
                    flowLazy()
                        .onEach { inner.emit(it) }
                        .inBackground()
                        .launchIn(this)
                }

                return@useCacheInternal { inner }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> useCacheInternal(
        key: String,
        scope: CoroutineScope,
        cachedAction: AwaitableConstructor<T>
    ): T {
        val awaitable = mutex.withLock {
            if (key in memory) {
                Log.d(LOG_TAG, "Key $key requested - already present")

                val entry = memory.getValue(key)

                entry.dependents += scope

                entry.awaitable
            } else {
                Log.d(LOG_TAG, "Key $key requested - creating new operation")

                val aggregateScope = CoroutineScope(Dispatchers.Default)
                val awaitable = cachedAction(aggregateScope)

                memory[key] = Entry(dependents = mutableSetOf(scope), aggregateScope, awaitable)

                awaitable
            }
        }

        scope.invokeOnCompletion {
            this@RealComputationalCache.launch {
                mutex.withLock {
                    memory[key]?.let { entry ->
                        entry.dependents -= scope

                        if (entry.dependents.isEmpty()) {
                            Log.d(this@RealComputationalCache.LOG_TAG, "Key $key - last scope cancelled")

                            memory.remove(key)

                            entry.aggregateScope.cancel()
                        } else {
                            Log.d(this@RealComputationalCache.LOG_TAG, "Key $key - scope cancelled, ${entry.dependents.size} remaining")
                        }
                    }
                }
            }
        }

        return awaitable() as T
    }
}
