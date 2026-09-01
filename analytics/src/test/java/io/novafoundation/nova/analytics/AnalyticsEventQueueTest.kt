package io.novafoundation.nova.analytics

import com.google.gson.Gson
import io.novafoundation.nova.analytics.transport.AnalyticsEventQueue
import io.novafoundation.nova.analytics.transport.QueuedEvent
import io.novafoundation.nova.core_db.dao.AnalyticsEventsDao
import io.novafoundation.nova.core_db.model.AnalyticsEventLocal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InMemoryAnalyticsEventsDao : AnalyticsEventsDao {

    private val rows = mutableListOf<AnalyticsEventLocal>()
    private var nextId = 1L

    override suspend fun insert(event: AnalyticsEventLocal) {
        rows += event.copy(id = nextId++)
    }

    override suspend fun peekOldest(limit: Int): List<AnalyticsEventLocal> =
        rows.sortedBy { it.id }.take(limit)

    override suspend fun deleteOldest(count: Int) {
        rows.sortedBy { it.id }.take(count).forEach { rows.remove(it) }
    }

    override suspend fun trimToNewest(maxSize: Int) {
        val keep = rows.sortedByDescending { it.id }.take(maxSize).toSet()
        rows.retainAll(keep)
    }

    override suspend fun count(): Int = rows.size

    override suspend fun clear() = rows.clear()
}

class AnalyticsEventQueueTest {

    private lateinit var dao: InMemoryAnalyticsEventsDao
    private lateinit var queue: AnalyticsEventQueue

    @Before
    fun setUp() {
        dao = InMemoryAnalyticsEventsDao()
        queue = AnalyticsEventQueue(dao, Gson(), maxSize = 5)
    }

    @Test
    fun `events survive being written and read back`() = runTest {
        queue.enqueue(QueuedEvent("app_opened", 1000L, mapOf("is_first_launch" to true)))

        val stored = queue.peek(10).single()

        assertEquals("app_opened", stored.name)
        assertEquals(1000L, stored.timestamp)
        assertEquals(true, stored.props["is_first_launch"])
    }

    @Test
    fun `peek preserves fifo order and drop removes the oldest`() = runTest {
        repeat(3) { queue.enqueue(QueuedEvent("event_$it", it.toLong(), emptyMap())) }

        assertEquals(listOf("event_0", "event_1", "event_2"), queue.peek(10).map { it.name })

        queue.drop(2)

        assertEquals(listOf("event_2"), queue.peek(10).map { it.name })
    }

    @Test
    fun `overflow keeps the newest events`() = runTest {
        repeat(8) { queue.enqueue(QueuedEvent("event_$it", it.toLong(), emptyMap())) }

        val names = queue.peek(10).map { it.name }

        assertEquals(5, names.size)
        assertEquals(listOf("event_3", "event_4", "event_5", "event_6", "event_7"), names)
    }

    @Test
    fun `clear empties the queue`() = runTest {
        queue.enqueue(QueuedEvent("event", 1L, emptyMap()))

        queue.clear()

        assertTrue(queue.peek(10).isEmpty())
        assertEquals(0, queue.size())
    }
}
