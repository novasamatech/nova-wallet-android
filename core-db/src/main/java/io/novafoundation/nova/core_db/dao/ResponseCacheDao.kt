package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.ResponseCacheKey
import io.novafoundation.nova.core_db.model.ResponseCacheLocal

@Dao
interface ResponseCacheDao {

    @Query("SELECT * FROM response_cache WHERE normalizedUrl = :url AND metaAccountId IS :metaAccountId")
    suspend fun get(url: String, metaAccountId: Long?): ResponseCacheLocal?

    @Query("SELECT EXISTS(SELECT 1 FROM meta_accounts WHERE id = :metaAccountId)")
    suspend fun ownerExists(metaAccountId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: ResponseCacheLocal)

    @Query("SELECT normalizedUrl, metaAccountId FROM response_cache")
    suspend fun keys(): List<ResponseCacheKey>

    @Query("DELETE FROM response_cache WHERE `key` IN (:keys)")
    suspend fun delete(keys: List<String>)

    @Query(
        """
        DELETE FROM response_cache WHERE `key` IN (
            SELECT `key` FROM response_cache WHERE expiresAt != 9223372036854775807
            ORDER BY fetchedAt DESC, `key` LIMIT -1 OFFSET :limit
        )
        """
    )
    suspend fun trim(limit: Int)
}
