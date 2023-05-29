package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.TotalRewardLocal
import kotlinx.coroutines.flow.Flow

@Dao
abstract class StakingTotalRewardDao {

    @Query("SELECT * FROM total_reward WHERE accountAddress = :accountAddress AND chainId = :chainId AND chainAssetId = :chainAssetId")
    abstract fun observeTotalRewards(accountAddress: String, chainId: String, chainAssetId: Int): Flow<TotalRewardLocal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(totalRewardLocal: TotalRewardLocal)

    @Query("DELETE FROM total_reward")
    abstract suspend fun deleteAll()
}
