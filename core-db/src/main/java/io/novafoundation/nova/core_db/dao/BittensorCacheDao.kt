package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.BittensorCacheLocal

@Dao
interface BittensorCacheDao {

    @Query("SELECT * FROM bittensor_cache WHERE `key` = :key")
    suspend fun get(key: String): BittensorCacheLocal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: BittensorCacheLocal)

    @Query("SELECT * FROM bittensor_cache WHERE namespace = :namespace AND generationAsOf IS NOT NULL")
    suspend fun generations(namespace: String): List<BittensorCacheLocal>

    @Query("DELETE FROM bittensor_cache WHERE namespace != :namespace")
    suspend fun keepNamespace(namespace: String)

    @Query("DELETE FROM bittensor_cache WHERE namespace = :namespace AND generationAsOf IS NOT NULL")
    suspend fun dropGenerations(namespace: String)

    @Query("DELETE FROM bittensor_cache WHERE namespace = :namespace AND account = :account AND resource IN (:resources)")
    suspend fun invalidate(namespace: String, account: String, resources: List<String>)

    @Query(
        """
        DELETE FROM bittensor_cache WHERE `key` IN (
            SELECT `key` FROM bittensor_cache WHERE resource != '__generation'
            ORDER BY fetchedAt DESC, `key` LIMIT -1 OFFSET :limit
        )
        """
    )
    suspend fun trim(limit: Int)
}
