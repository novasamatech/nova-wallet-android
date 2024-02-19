package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.TotalRewardLocal
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow

@Dao
abstract class StakingTotalRewardDao {

    @Query(
        """
        SELECT * FROM total_reward
        WHERE accountId = :accountId AND chainId = :chainId AND chainAssetId = :chainAssetId and stakingType = :stakingType
        """
    )
    abstract fun observeTotalRewards(accountId: AccountId, chainId: String, chainAssetId: Int, stakingType: String): Flow<TotalRewardLocal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(totalRewardLocal: TotalRewardLocal)

    @Query("DELETE FROM total_reward")
    abstract suspend fun deleteAll()
}
