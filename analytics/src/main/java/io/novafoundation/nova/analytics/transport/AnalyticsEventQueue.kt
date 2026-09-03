package io.novafoundation.nova.analytics.transport

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.novafoundation.nova.core_db.dao.AnalyticsEventsDao
import io.novafoundation.nova.core_db.model.AnalyticsEventLocal

class QueuedEvent(
    val id: String,
    val name: String,
    val timestamp: Long,
    val props: Map<String, Any?>
)

class AnalyticsEventQueue(
    private val dao: AnalyticsEventsDao,
    private val gson: Gson,
    private val maxSize: Int
) {

    private val propsType = object : TypeToken<Map<String, Any?>>() {}.type

    suspend fun enqueue(event: QueuedEvent) {
        dao.insert(
            AnalyticsEventLocal(
                eventId = event.id,
                name = event.name,
                timestamp = event.timestamp,
                propsJson = gson.toJson(event.props)
            )
        )
        dao.trimToNewest(maxSize)
    }

    suspend fun peek(limit: Int): List<QueuedEvent> {
        return dao.peekOldest(limit).map { local ->
            QueuedEvent(
                id = local.eventId,
                name = local.name,
                timestamp = local.timestamp,
                props = gson.fromJson(local.propsJson, propsType) ?: emptyMap()
            )
        }
    }

    suspend fun drop(count: Int) = dao.deleteOldest(count)

    suspend fun size(): Int = dao.count()

    suspend fun clear() = dao.clear()
}
