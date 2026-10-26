package io.novafoundation.nova.analytics

import com.google.gson.Gson
import io.novafoundation.nova.analytics.transport.AnalyticsApi
import io.novafoundation.nova.analytics.transport.AnalyticsEventQueue
import io.novafoundation.nova.analytics.transport.AnalyticsEventsRequest
import io.novafoundation.nova.analytics.transport.AnalyticsUploader
import io.novafoundation.nova.analytics.transport.QueuedEvent
import io.novafoundation.nova.analytics.transport.RealAnalyticsIdentity
import io.novafoundation.nova.analytics.transport.RealAnalyticsService
import io.novafoundation.nova.core_db.dao.AnalyticsEventsDao
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsConsentTest {

    @Test
    fun `disabled launch erases persisted analytics without resolving an upload URL`() = runTest {
        val fixture = Fixture(this)
        val oldId = fixture.identity.installId()
        val event = QueuedEvent("old", "session_started", 0L, emptyMap())
        fixture.queue.enqueue(event)

        fixture.service.isEnabled = false
        fixture.service.track(AnalyticsEvent.SessionStarted)
        fixture.service.flush(AnalyticsFlushReason.LAUNCH)
        advanceUntilIdle()

        assertFalse(fixture.service.isEnabled)
        assertEquals(0, fixture.queue.size())
        assertTrue(fixture.api.requests.isEmpty())
        assertEquals(0, fixture.urlReads)
        assertNotEquals(oldId, fixture.identity.installId())
    }

    @Test
    fun `queued tracking from withdrawn consent cannot enter a later session`() = runTest {
        val fixture = Fixture(this)
        fixture.service.isEnabled = true
        fixture.service.track(AnalyticsEvent.SessionStarted)
        fixture.service.isEnabled = false
        fixture.service.isEnabled = true
        fixture.service.track(AnalyticsEvent.WalletCreationStarted)
        advanceUntilIdle()

        fixture.service.flush(AnalyticsFlushReason.LAUNCH)

        assertEquals(listOf("wallet_creation_started"), fixture.api.requests.single().events.map { it.name })
        assertEquals(0, fixture.queue.size())
    }

    @Test
    fun `withdrawal while resolving URL cancels old upload even after immediate reenable`() = runTest {
        val releaseUrl = CompletableDeferred<Unit>()
        val fixture = Fixture(this)
        fixture.beforeUrl = { withContext(NonCancellable) { releaseUrl.await() } }
        fixture.service.isEnabled = true
        fixture.service.track(AnalyticsEvent.SessionStarted)
        advanceUntilIdle()
        val upload = launch { fixture.service.flush(AnalyticsFlushReason.LAUNCH) }
        runCurrent()
        assertEquals(1, fixture.urlReads)

        fixture.service.isEnabled = false
        fixture.service.isEnabled = true
        releaseUrl.complete(Unit)
        advanceUntilIdle()

        assertTrue(upload.isCancelled)
        assertTrue(fixture.api.requests.isEmpty())
        assertEquals(0, fixture.queue.size())

        fixture.beforeUrl = {}
        fixture.service.track(AnalyticsEvent.WalletCreationStarted)
        advanceUntilIdle()
        fixture.service.flush(AnalyticsFlushReason.LAUNCH)
        assertEquals(listOf("wallet_creation_started"), fixture.api.requests.single().events.map { it.name })
    }

    @Test
    fun `withdrawal cancels in flight upload and prevents the next batch`() = runTest {
        val fixture = Fixture(this)
        var cancelled = false
        fixture.api.onSend = {
            try {
                awaitCancellation()
            } finally {
                cancelled = true
            }
        }
        fixture.service.isEnabled = true
        repeat(3) { fixture.service.track(AnalyticsEvent.SessionStarted) }
        advanceUntilIdle()
        val upload = launch { fixture.service.flush(AnalyticsFlushReason.LAUNCH) }
        runCurrent()
        assertEquals(1, fixture.api.requests.size)

        fixture.service.isEnabled = false
        advanceUntilIdle()

        assertTrue(cancelled)
        assertTrue(upload.isCancelled)
        assertEquals(1, fixture.api.requests.size)
        assertEquals(0, fixture.queue.size())
    }

    @Test
    fun `new consent waits for erasure and its events survive delayed clearing`() = runTest {
        val backingDao = InMemoryAnalyticsEventsDao()
        val releaseClear = CompletableDeferred<Unit>()
        val dao = object : AnalyticsEventsDao by backingDao {
            override suspend fun clear() {
                releaseClear.await()
                backingDao.clear()
            }
        }
        val fixture = Fixture(this, dao)
        fixture.service.isEnabled = true
        fixture.service.track(AnalyticsEvent.SessionStarted)
        advanceUntilIdle()
        fixture.service.isEnabled = false
        fixture.service.isEnabled = true
        fixture.service.track(AnalyticsEvent.WalletCreationStarted)
        val upload = launch { fixture.service.flush(AnalyticsFlushReason.LAUNCH) }
        runCurrent()

        assertTrue(fixture.api.requests.isEmpty())
        assertEquals(0, fixture.urlReads)
        releaseClear.complete(Unit)
        advanceUntilIdle()
        upload.join()
        // If flush wins the queue lock, the new event remains queued for the next flush.
        fixture.service.flush(AnalyticsFlushReason.LAUNCH)
        assertEquals(listOf("wallet_creation_started"), fixture.api.requests.flatMap { it.events }.map { it.name })
    }

    @Test
    fun `transient failure retains event identity and later flush drains batches`() = runTest {
        val fixture = Fixture(this)
        fixture.service.isEnabled = true
        repeat(3) { fixture.service.track(AnalyticsEvent.SessionStarted) }
        advanceUntilIdle()
        fixture.api.onSend = { throw IOException("offline") }
        fixture.service.flush(AnalyticsFlushReason.LAUNCH)
        val first = fixture.api.requests.single()
        assertEquals(3, fixture.queue.size())

        fixture.api.onSend = {}
        fixture.service.flush(AnalyticsFlushReason.LAUNCH)

        assertEquals(first.install_id, fixture.api.requests[1].install_id)
        assertEquals(first.events.map { it.id }, fixture.api.requests[1].events.map { it.id })
        assertEquals(listOf(2, 2, 1), fixture.api.requests.map { it.events.size })
        assertEquals(0, fixture.queue.size())
    }

    private class RecordingApi : AnalyticsApi {
        val requests = mutableListOf<AnalyticsEventsRequest>()

        var onSend: suspend () -> Unit = {}

        override suspend fun sendEvents(url: String, body: AnalyticsEventsRequest) {
            requests += body
            onSend()
        }
    }

    private class Fixture(scope: TestScope, suppliedDao: AnalyticsEventsDao? = null) {
        val preferences = InMemoryPreferences()

        val identity = RealAnalyticsIdentity(preferences)

        private val dao = suppliedDao ?: InMemoryAnalyticsEventsDao()

        private val gson = Gson()

        val queue = AnalyticsEventQueue(dao, gson, maxSize = 50)

        val api = RecordingApi()

        var urlReads = 0

        var beforeUrl: suspend () -> Unit = {}

        private val urls = InfrastructureUrls {
            urlReads++
            beforeUrl()
            "https://example.test/".toHttpUrl()
        }

        private val uploader = AnalyticsUploader(api, urls, identity, queue, "test", batchSize = 2)

        private val dispatcher = StandardTestDispatcher(scope.testScheduler)

        private val clock = { 0L }

        val service = RealAnalyticsService(
            scope = scope,
            queue = queue,
            uploader = uploader,
            identity = identity,
            flushThreshold = 50,
            clock = clock,
            dispatcher = dispatcher
        )
    }
}
