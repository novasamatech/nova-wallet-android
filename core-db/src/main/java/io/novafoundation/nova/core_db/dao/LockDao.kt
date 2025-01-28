package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.novafoundation.nova.core_db.model.BalanceLockLocal
import kotlinx.coroutines.flow.Flow

@Dao
abstract class LockDao {

    @Transaction
    open suspend fun updateLocks(
        locks: List<BalanceLockLocal>,
        metaId: Long,
        chainId: String,
        chainAssetId: Int
    ) {
        deleteLocks(metaId, chainId, chainAssetId)

        insert(locks)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(locks: List<BalanceLockLocal>)

    @Query("DELETE FROM locks WHERE metaId = :metaId AND chainId = :chainId AND assetId = :chainAssetId")
    abstract fun deleteLocks(metaId: Long, chainId: String, chainAssetId: Int)

    @Query("SELECT * FROM locks WHERE metaId = :metaId")
    abstract fun observeLocksForMetaAccount(metaId: Long): Flow<List<BalanceLockLocal>>

    @Query("SELECT * FROM locks WHERE metaId = :metaId AND chainId = :chainId AND assetId = :chainAssetId")
    abstract fun observeBalanceLocks(metaId: Long, chainId: String, chainAssetId: Int): Flow<List<BalanceLockLocal>>

    @Query("SELECT * FROM locks WHERE metaId = :metaId AND chainId = :chainId AND assetId = :chainAssetId")
    abstract suspend fun getBalanceLocks(metaId: Long, chainId: String, chainAssetId: Int): List<BalanceLockLocal>

    @Query(
        """
        SELECT * FROM locks
        WHERE metaId = :metaId AND chainId = :chainId AND assetId = :chainAssetId
        ORDER BY amount DESC
        LIMIT 1
    """
    )
    abstract suspend fun getBiggestBalanceLock(metaId: Long, chainId: String, chainAssetId: Int): BalanceLockLocal?

    @Query("SELECT * FROM locks WHERE metaId = :metaId AND chainId = :chainId AND assetId = :chainAssetId AND type = :lockId")
    abstract fun observeBalanceLock(metaId: Long, chainId: String, chainAssetId: Int, lockId: String): Flow<BalanceLockLocal?>
}
