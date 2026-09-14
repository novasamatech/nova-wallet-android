package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.novafoundation.nova.core_db.model.AnalyticsEventLocal

@Dao
interface AnalyticsEventsDao {

    @Insert
    suspend fun insert(event: AnalyticsEventLocal)

    @Query("SELECT * FROM analytics_pending_events ORDER BY id LIMIT :limit")
    suspend fun peekOldest(limit: Int): List<AnalyticsEventLocal>

    @Query("DELETE FROM analytics_pending_events WHERE id IN (SELECT id FROM analytics_pending_events ORDER BY id LIMIT :count)")
    suspend fun deleteOldest(count: Int)

    @Query("DELETE FROM analytics_pending_events WHERE id NOT IN (SELECT id FROM analytics_pending_events ORDER BY id DESC LIMIT :maxSize)")
    suspend fun trimToNewest(maxSize: Int)

    @Query("SELECT COUNT(*) FROM analytics_pending_events")
    suspend fun count(): Int

    @Query("DELETE FROM analytics_pending_events")
    suspend fun clear()
}
