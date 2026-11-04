package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.data.cache.refreshOnForegroundOrInterval
import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RefreshFlowTest {

    @Test
    fun `refresh starts immediately and success interval starts after completion`() = runTest {
        val foreground = MutableSharedFlow<Unit>()
        val startedAt = mutableListOf<Long>()
        val updates = mutableListOf<Int>()
        val refresh: suspend () -> Result<Int> = {
            startedAt += testScheduler.currentTime
            delay(5_000)
            Result.success(startedAt.size)
        }
        val flow = foreground.refreshOnForegroundOrInterval(300.seconds, 30.seconds, refresh)
        val observer = backgroundScope.launch { flow.toList(updates) }

        runCurrent()
        assertEquals(listOf(0L), startedAt)
        assertTrue(updates.isEmpty())
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(listOf(1), updates)

        advanceTimeBy(299_999)
        runCurrent()
        assertEquals(listOf(0L), startedAt)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(0L, 305_000L), startedAt)
        assertEquals(listOf(1), updates)
        observer.cancelAndJoin()
    }

    @Test
    fun `failed refresh emits nothing and retries after the shorter interval`() = runTest {
        val foreground = MutableSharedFlow<Unit>()
        val startedAt = mutableListOf<Long>()
        val updates = mutableListOf<Int>()
        val refresh: suspend () -> Result<Int> = {
            startedAt += testScheduler.currentTime
            delay(7_000)
            if (startedAt.size == 1) Result.failure(IOException("Offline")) else Result.success(startedAt.size)
        }
        val flow = foreground.refreshOnForegroundOrInterval(300.seconds, 30.seconds, refresh)
        val observer = backgroundScope.launch { flow.toList(updates) }

        runCurrent()
        advanceTimeBy(7_000)
        runCurrent()
        assertTrue(updates.isEmpty())
        advanceTimeBy(29_999)
        runCurrent()
        assertEquals(listOf(0L), startedAt)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(0L, 37_000L), startedAt)

        advanceTimeBy(7_000)
        runCurrent()
        assertEquals(listOf(2), updates)
        advanceTimeBy(299_999)
        runCurrent()
        assertEquals(2, startedAt.size)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(0L, 37_000L, 344_000L), startedAt)
        observer.cancelAndJoin()
    }

    @Test
    fun `foreground wakes an idle wait and cancellation removes its subscription`() = runTest {
        val foreground = MutableSharedFlow<Unit>()
        var requests = 0
        val updates = mutableListOf<Int>()
        val refresh: suspend () -> Result<Int> = { Result.success(++requests) }
        val flow = foreground.refreshOnForegroundOrInterval(300.seconds, 30.seconds, refresh)
        val observer = backgroundScope.launch { flow.toList(updates) }

        runCurrent()
        assertEquals(listOf(1), updates)
        assertEquals(1, foreground.subscriptionCount.value)
        advanceTimeBy(10_000)
        foreground.emit(Unit)
        runCurrent()
        assertEquals(listOf(1, 2), updates)
        assertEquals(1, foreground.subscriptionCount.value)

        observer.cancelAndJoin()
        assertEquals(0, foreground.subscriptionCount.value)
        foreground.emit(Unit)
        advanceTimeBy(600_000)
        runCurrent()
        assertEquals(2, requests)
    }

    @Test
    fun `foreground during an active request neither cancels nor queues another refresh`() = runTest {
        val foreground = MutableSharedFlow<Unit>()
        val release = CompletableDeferred<Unit>()
        var requests = 0
        var requestCancelled = false
        val updates = mutableListOf<Int>()
        val refresh: suspend () -> Result<Int> = {
            requests++
            try {
                release.await()
                Result.success(requests)
            } catch (cancelled: CancellationException) {
                requestCancelled = true
                throw cancelled
            }
        }
        val flow = foreground.refreshOnForegroundOrInterval(300.seconds, 30.seconds, refresh)
        val observer = backgroundScope.launch { flow.toList(updates) }

        runCurrent()
        assertEquals(1, requests)
        assertEquals(0, foreground.subscriptionCount.value)
        repeat(3) { foreground.emit(Unit) }
        runCurrent()
        assertFalse(requestCancelled)
        assertEquals(1, requests)

        release.complete(Unit)
        runCurrent()
        assertEquals(listOf(1), updates)
        assertEquals(1, requests)
        assertFalse(requestCancelled)
        advanceTimeBy(299_999)
        runCurrent()
        assertEquals(1, requests)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(1, 2), updates)
        observer.cancelAndJoin()
    }

    @Test
    fun `cancellation propagates whether thrown or returned as a failed result`() = runTest {
        for (returnedAsFailure in listOf(false, true)) {
            val foreground = MutableSharedFlow<Unit>()
            val cancellation = CancellationException("Refresh cancelled")
            var requests = 0
            val refresh: suspend () -> Result<Int> = {
                requests++
                if (returnedAsFailure) Result.failure(cancellation) else throw cancellation
            }
            val flow = foreground.refreshOnForegroundOrInterval(300.seconds, 30.seconds, refresh)
            val observation = async { runCatching { flow.first() } }

            assertSame(cancellation, observation.await().exceptionOrNull())
            advanceTimeBy(600_000)
            runCurrent()
            assertEquals(1, requests)
            assertEquals(0, foreground.subscriptionCount.value)
        }
    }

    @Test
    fun `cancelling collection cancels its active request and prevents later emissions`() = runTest {
        val foreground = MutableSharedFlow<Unit>()
        val release = CompletableDeferred<Int>()
        var requests = 0
        var requestCancelled = false
        val updates = mutableListOf<Int>()
        val refresh: suspend () -> Result<Int> = {
            requests++
            try {
                Result.success(release.await())
            } catch (cancelled: CancellationException) {
                requestCancelled = true
                throw cancelled
            }
        }
        val flow = foreground.refreshOnForegroundOrInterval(300.seconds, 30.seconds, refresh)
        val observer = backgroundScope.launch { flow.toList(updates) }

        runCurrent()
        observer.cancelAndJoin()
        assertTrue(requestCancelled)
        release.complete(1)
        advanceTimeBy(600_000)
        runCurrent()
        assertEquals(1, requests)
        assertTrue(updates.isEmpty())
        assertEquals(0, foreground.subscriptionCount.value)
    }

    @Test
    fun `completed foreground stream keeps periodic refresh without a busy loop`() = runTest {
        val foreground = emptyFlow<Unit>()
        var requests = 0
        val updates = mutableListOf<Int>()
        val refresh: suspend () -> Result<Int> = { Result.success(++requests) }
        val flow = foreground.refreshOnForegroundOrInterval(300.seconds, 30.seconds, refresh)
        val observer = backgroundScope.launch { flow.toList(updates) }

        runCurrent()
        assertEquals(listOf(1), updates)
        advanceTimeBy(299_999)
        runCurrent()
        assertEquals(1, requests)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(1, 2), updates)
        observer.cancelAndJoin()
    }
}
