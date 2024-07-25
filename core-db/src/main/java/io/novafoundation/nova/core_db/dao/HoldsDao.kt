package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.novafoundation.nova.core_db.model.BalanceHoldLocal
import kotlinx.coroutines.flow.Flow

@Dao
abstract class HoldsDao {

    @Transaction
    open suspend fun updateHolds(
        holds: List<BalanceHoldLocal>,
        metaId: Long,
        chainId: String,
        chainAssetId: Int
    ) {
        deleteHolds(metaId, chainId, chainAssetId)

        insert(holds)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insert(holds: List<BalanceHoldLocal>)

    @Query("DELETE FROM holds WHERE metaId = :metaId AND chainId = :chainId AND assetId = :chainAssetId")
    protected abstract fun deleteHolds(metaId: Long, chainId: String, chainAssetId: Int)

    @Query("SELECT * FROM holds WHERE metaId = :metaId")
    abstract fun observeHoldsForMetaAccount(metaId: Long): Flow<List<BalanceHoldLocal>>

    @Query("SELECT * FROM holds WHERE metaId = :metaId AND chainId = :chainId AND assetId = :chainAssetId")
    abstract fun observeBalanceHolds(metaId: Long, chainId: String, chainAssetId: Int): Flow<List<BalanceHoldLocal>>
}
