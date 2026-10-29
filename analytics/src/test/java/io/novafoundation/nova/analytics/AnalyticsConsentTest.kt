package io.novafoundation.nova.analytics

import com.google.gson.Gson
import io.novafoundation.nova.analytics.transport.AnalyticsApi
import io.novafoundation.nova.analytics.transport.AnalyticsEventQueue
import io.novafoundation.nova.analytics.transport.AnalyticsEventsRequest
import io.novafoundation.nova.analytics.transport.AnalyticsIdentity
import io.novafoundation.nova.analytics.transport.AnalyticsUploader
import io.novafoundation.nova.analytics.transport.QueuedEvent
import io.novafoundation.nova.analytics.transport.RealAnalyticsIdentity
import io.novafoundation.nova.analytics.transport.RealAnalyticsService
import io.novafoundation.nova.core_db.dao.AnalyticsEventsDao
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        val event = QueuedEvent(id = "old", name = "session_started", timestamp = 0L, props = emptyMap())
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
    fun `failed first clear is retried by later disable and only new consent events upload`() = runTest {
        val backingDao = InMemoryAnalyticsEventsDao()
        val dao = ControllableErasureDao(backingDao)
        dao.beforeClear = { if (dao.clearAttempts == 1) throw IOException("storage unavailable") }
        // Like RootScope, backgroundScope supervises failures so the old implementation can
        // reach the later disable and expose its poisoned erasure instead of cancelling this test.
        val fixture = Fixture(this, dao, serviceScope = backgroundScope)
        val oldId = fixture.identity.installId()
        fixture.service.isEnabled = true
        fixture.service.track(AnalyticsEvent.SessionStarted)
        runCurrent()

        fixture.service.isEnabled = false
        runCurrent()
        assertEquals(1, dao.clearAttempts)
        assertEquals(listOf("session_started"), fixture.queue.peek(limit = 50).map { it.name })
        assertEquals(oldId, fixture.identity.installId())

        fixture.service.isEnabled = true
        fixture.service.track(AnalyticsEvent.SessionStarted)
        fixture.service.isEnabled = false
        fixture.service.isEnabled = true
        fixture.service.track(AnalyticsEvent.WalletCreationStarted)
        runCurrent()
        assertEquals(2, dao.clearAttempts)
        fixture.service.flush(AnalyticsFlushReason.LAUNCH)

        val request = fixture.api.requests.single()
        assertEquals(listOf("wallet_creation_started"), request.events.map { it.name })
        assertNotEquals(oldId, request.install_id)
        assertEquals(fixture.identity.installId(), request.install_id)
        assertEquals(0, fixture.queue.size())
    }

    @Test
    fun `failed erasure blocks tracking and flush until tracking retries after storage recovery`() = runTest {
        val backingDao = InMemoryAnalyticsEventsDao()
        val dao = ControllableErasureDao(backingDao)
        val fixture = Fixture(this, dao)
        val oldId = fixture.identity.installId()
        fixture.service.isEnabled = true
        fixture.service.track(AnalyticsEvent.SessionStarted)
        advanceUntilIdle()
        dao.beforeClear = { throw IOException("storage unavailable") }

        fixture.service.isEnabled = false
        runCurrent()
        fixture.service.isEnabled = true
        fixture.service.track(AnalyticsEvent.WalletCreationStarted)
        advanceUntilIdle()
        fixture.service.flush(AnalyticsFlushReason.LAUNCH)

        assertEquals(3, dao.clearAttempts)
        assertTrue(fixture.service.isEnabled)
        assertEquals(listOf("session_started"), fixture.queue.peek(limit = 50).map { it.name })
        assertEquals(oldId, fixture.identity.installId())
        assertTrue(fixture.api.requests.isEmpty())
        assertEquals(0, fixture.urlReads)

        dao.beforeClear = {}
        fixture.service.track(AnalyticsEvent.WalletCreationStarted)
        advanceUntilIdle()
        fixture.service.flush(AnalyticsFlushReason.LAUNCH)

        assertEquals(4, dao.clearAttempts)
        val request = fixture.api.requests.single()
        assertEquals(listOf("wallet_creation_started"), request.events.map { it.name })
        assertNotEquals(oldId, request.install_id)
        assertEquals(0, fixture.queue.size())
    }

    @Test
    fun `flush retries failed erasure before reading obsolete events`() = runTest {
        val backingDao = InMemoryAnalyticsEventsDao()
        val dao = ControllableErasureDao(backingDao)
        dao.beforeClear = { if (dao.clearAttempts == 1) throw IOException("storage unavailable") }
        val fixture = Fixture(this, dao)
        val oldId = fixture.identity.installId()
        val oldEvent = QueuedEvent(id = "old", name = "session_started", timestamp = 0L, props = emptyMap())
        fixture.queue.enqueue(oldEvent)
        fixture.service.isEnabled = false
        runCurrent()

        fixture.service.isEnabled = true
        fixture.service.flush(AnalyticsFlushReason.LAUNCH)

        assertEquals(2, dao.clearAttempts)
        assertEquals(0, fixture.queue.size())
        assertNotEquals(oldId, fixture.identity.installId())
        assertTrue(fixture.api.requests.isEmpty())
        assertEquals(0, fixture.urlReads)

        fixture.service.track(AnalyticsEvent.WalletCreationStarted)
        advanceUntilIdle()
        fixture.service.flush(AnalyticsFlushReason.LAUNCH)
        assertEquals(listOf("wallet_creation_started"), fixture.api.requests.single().events.map { it.name })
        assertEquals(2, dao.clearAttempts)
    }

    @Test
    fun `superseded erasure cannot clear events admitted after current erasure`() = runTest {
        val fixture = Fixture(this)
        val oldEvent = QueuedEvent(id = "old", name = "session_started", timestamp = 0L, props = emptyMap())
        val newEvent = QueuedEvent(id = "new", name = "wallet_creation_started", timestamp = 0L, props = emptyMap())
        fixture.queue.enqueue(oldEvent)
        fixture.service.isEnabled = false
        fixture.service.isEnabled = false
        fixture.service.isEnabled = true

        // Complete the current gate before either scheduled background erasure runs,
        // then model an event persisted under that successfully renewed consent.
        val flushDispatcher = UnconfinedTestDispatcher(testScheduler)
        withContext(flushDispatcher) {
            fixture.service.flush(AnalyticsFlushReason.LAUNCH)
            fixture.queue.enqueue(newEvent)
        }
        runCurrent()

        assertEquals(listOf("new"), fixture.queue.peek(limit = 50).map { it.id })
        fixture.service.flush(AnalyticsFlushReason.LAUNCH)
        assertEquals(listOf("new"), fixture.api.requests.single().events.map { it.id })
    }

    @Test
    fun `failed identity rotation blocks new events until erasure retry completes`() = runTest {
        val preferences = InMemoryPreferences()
        val backingIdentity = RealAnalyticsIdentity(preferences)
        var rotationUnavailable = true
        val identity = object : AnalyticsIdentity by backingIdentity {
            override fun resetInstallId() {
                if (rotationUnavailable) throw IOException("identity storage unavailable")
                backingIdentity.resetInstallId()
            }
        }
        val fixture = Fixture(this, suppliedIdentity = identity)
        val oldId = identity.installId()
        val oldEvent = QueuedEvent(id = "old", name = "session_started", timestamp = 0L, props = emptyMap())
        fixture.queue.enqueue(oldEvent)
        fixture.service.isEnabled = false
        runCurrent()
        fixture.service.isEnabled = true
        fixture.service.track(AnalyticsEvent.SessionStarted)
        advanceUntilIdle()
        fixture.service.flush(AnalyticsFlushReason.LAUNCH)

        assertEquals(0, fixture.queue.size())
        assertEquals(oldId, identity.installId())
        assertTrue(fixture.api.requests.isEmpty())
        assertEquals(0, fixture.urlReads)

        rotationUnavailable = false
        fixture.service.track(AnalyticsEvent.WalletCreationStarted)
        advanceUntilIdle()
        fixture.service.flush(AnalyticsFlushReason.LAUNCH)
        val request = fixture.api.requests.single()
        assertEquals(listOf("wallet_creation_started"), request.events.map { it.name })
        assertNotEquals(oldId, request.install_id)
    }

    @Test
    fun `withdrawal cancels erasure retry without poisoning the next consent`() = runTest {
        val backingDao = InMemoryAnalyticsEventsDao()
        val dao = ControllableErasureDao(backingDao)
        var retryCancelled = false
        dao.beforeClear = {
            when (dao.clearAttempts) {
                1 -> throw IOException("storage unavailable")
                2 -> try {
                    awaitCancellation()
                } finally {
                    retryCancelled = true
                }
            }
        }
        val fixture = Fixture(this, dao)
        fixture.service.isEnabled = false
        runCurrent()
        fixture.service.isEnabled = true
        val upload = launch { fixture.service.flush(AnalyticsFlushReason.LAUNCH) }
        runCurrent()
        assertEquals(2, dao.clearAttempts)

        fixture.service.isEnabled = false
        fixture.service.isEnabled = true
        fixture.service.track(AnalyticsEvent.WalletCreationStarted)
        advanceUntilIdle()
        fixture.service.flush(AnalyticsFlushReason.LAUNCH)

        assertTrue(retryCancelled)
        assertTrue(upload.isCancelled)
        assertEquals(3, dao.clearAttempts)
        assertEquals(listOf("wallet_creation_started"), fixture.api.requests.single().events.map { it.name })
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

    private class ControllableErasureDao(private val backingDao: AnalyticsEventsDao) : AnalyticsEventsDao by backingDao {
        var clearAttempts = 0
            private set

        var beforeClear: suspend () -> Unit = {}

        override suspend fun clear() {
            clearAttempts++
            beforeClear()
            backingDao.clear()
        }
    }

    private class Fixture(
        scope: TestScope,
        suppliedDao: AnalyticsEventsDao? = null,
        suppliedIdentity: AnalyticsIdentity? = null,
        serviceScope: CoroutineScope = scope
    ) {
        val preferences = InMemoryPreferences()

        val identity = suppliedIdentity ?: RealAnalyticsIdentity(preferences)

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

        private val uploader = AnalyticsUploader(api, urls, identity, queue, appVersion = "test", batchSize = 2)

        private val dispatcher = StandardTestDispatcher(scope.testScheduler)

        private val clock = { 0L }

        val service = RealAnalyticsService(
            scope = serviceScope,
            queue = queue,
            uploader = uploader,
            identity = identity,
            flushThreshold = 50,
            clock = clock,
            dispatcher = dispatcher
        )
    }
}
