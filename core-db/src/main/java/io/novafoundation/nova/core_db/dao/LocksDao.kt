package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.BalanceLockLocal
import kotlinx.coroutines.flow.Flow

@Dao
abstract class LocksDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(locks: List<BalanceLockLocal>)

    @Query("SELECT * FROM locks WHERE metaId = :metaId")
    abstract fun observeLocksForMetaAccount(metaId: Long): Flow<List<BalanceLockLocal>>

    @Query("SELECT * FROM locks WHERE metaId = :metaId AND chainId = :chainId AND assetId = :chainAssetId")
    abstract fun observeBalanceLocks(metaId: Long, chainId: String, chainAssetId: Int): Flow<List<BalanceLockLocal>>
}
