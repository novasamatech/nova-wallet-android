package io.novafoundation.nova.common.data.memory

import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ComputationalCacheFlowTest {

    @Test
    fun `observation is lazy and delegates its key and producer unchanged`() = runTest {
        val cache = ScopeRecordingCache()
        var producerCalls = 0
        val producer: suspend CoroutineScope.() -> Flow<Int> = {
            producerCalls++
            flowOf(7)
        }
        val observation = cache.observeSharedFlow("positions", producer)

        assertTrue(cache.calls.isEmpty())
        assertEquals(0, producerCalls)
        assertEquals(7, observation.first())
        assertEquals(1, producerCalls)
        assertEquals("positions", cache.calls.single().key)
        assertSame(producer, cache.calls.single().producer)
    }

    @Test
    fun `collectors register independent lifetimes and resubscription gets a new scope`() = runTest {
        val cache = ScopeRecordingCache()
        val upstream = MutableStateFlow(1)
        val producer: suspend CoroutineScope.() -> Flow<Int> = { upstream }
        val observation = cache.observeSharedFlow("positions", producer)
        val firstValues = mutableListOf<Int>()
        val secondValues = mutableListOf<Int>()
        val first = backgroundScope.launch { observation.toList(firstValues) }
        val second = backgroundScope.launch { observation.toList(secondValues) }
        runCurrent()

        assertEquals(listOf("positions", "positions"), cache.calls.map { it.key })
        val firstScope = cache.calls[0].scope
        val secondScope = cache.calls[1].scope
        assertNotSame(firstScope, secondScope)
        assertTrue(firstScope.isActive)
        assertTrue(secondScope.isActive)

        first.cancelAndJoin()
        assertFalse(firstScope.isActive)
        assertTrue(secondScope.isActive)
        upstream.value = 2
        runCurrent()
        assertEquals(listOf(1), firstValues)
        assertEquals(listOf(1, 2), secondValues)

        second.cancelAndJoin()
        assertTrue(firstScope.coroutineContext.job.isCompleted)
        assertTrue(secondScope.coroutineContext.job.isCompleted)
        val thirdValues = mutableListOf<Int>()
        val third = backgroundScope.launch { observation.toList(thirdValues) }
        runCurrent()
        val thirdScope = cache.calls[2].scope
        assertNotSame(firstScope, thirdScope)
        assertNotSame(secondScope, thirdScope)
        assertTrue(thirdScope.isActive)
        assertEquals(listOf(2), thirdValues)
        third.cancelAndJoin()
        assertTrue(thirdScope.coroutineContext.job.isCompleted)
    }

    @Test
    fun `first releases its registered scope while the caller remains active`() = runTest {
        val cache = ScopeRecordingCache()
        val producer: suspend CoroutineScope.() -> Flow<Int> = {
            flow {
                emit(1)
                awaitCancellation()
            }
        }
        val observation = cache.observeSharedFlow("positions", producer)

        assertEquals(1, observation.first())

        val registeredScope = cache.calls.single().scope
        assertTrue(registeredScope.coroutineContext.job.isCompleted)
        assertFalse(registeredScope.isActive)
        assertTrue(currentCoroutineContext().isActive)
    }

    @Test
    fun `normal completion releases its registered scope without cancelling the caller`() = runTest {
        val cache = ScopeRecordingCache()
        val producer: suspend CoroutineScope.() -> Flow<Int> = { flowOf(1, 2) }
        val observation = cache.observeSharedFlow("positions", producer)

        assertEquals(listOf(1, 2), observation.toList())

        assertTrue(cache.calls.single().scope.coroutineContext.job.isCompleted)
        assertTrue(currentCoroutineContext().isActive)
    }

    @Test
    fun `producer failure propagates and releases its registered scope`() = runTest {
        val cache = ScopeRecordingCache()
        val failure = IOException("Source unavailable")
        val producer: suspend CoroutineScope.() -> Flow<Int> = { throw failure }
        val observation = cache.observeSharedFlow("positions", producer)

        val result = runCatching { observation.first() }

        // Coroutine stack-trace recovery may copy the exception across the scope boundary.
        val propagated = requireNotNull(result.exceptionOrNull())
        assertEquals(failure.javaClass, propagated.javaClass)
        assertEquals(failure.message, propagated.message)
        assertTrue(cache.calls.single().scope.coroutineContext.job.isCompleted)
        assertTrue(currentCoroutineContext().isActive)
    }

    /** Records the helper's lifetime contract; production sharing belongs to RealComputationalCache. */
    private class ScopeRecordingCache : ComputationalCache {

        data class Call(val key: String, val scope: CoroutineScope, val producer: Any)

        val calls = mutableListOf<Call>()

        override suspend fun <T> useCache(key: String, scope: CoroutineScope, computation: suspend CoroutineScope.() -> T): T =
            error("Unexpected value-cache access")

        override fun <T> useSharedFlow(key: String, scope: CoroutineScope, flowLazy: suspend CoroutineScope.() -> Flow<T>): Flow<T> {
            calls += Call(key, scope, flowLazy)
            return flow { emitAll(flowLazy(scope)) }
        }
    }
}
