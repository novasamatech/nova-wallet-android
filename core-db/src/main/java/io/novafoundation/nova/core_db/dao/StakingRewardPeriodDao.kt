package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.StakingRewardPeriodLocal
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow

@Dao
interface StakingRewardPeriodDao {

    @Query("SELECT * FROM staking_reward_period WHERE accountId = :accountId AND chainId = :chainId AND assetId = :assetId AND stakingType = :stakingType")
    suspend fun getStakingRewardPeriod(accountId: AccountId, chainId: String, assetId: Int, stakingType: String): StakingRewardPeriodLocal?

    @Query("SELECT * FROM staking_reward_period WHERE accountId = :accountId AND chainId = :chainId AND assetId = :assetId AND stakingType = :stakingType")
    fun observeStakingRewardPeriod(accountId: AccountId, chainId: String, assetId: Int, stakingType: String): Flow<StakingRewardPeriodLocal?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStakingRewardPeriod(stakingRewardPeriodLocal: StakingRewardPeriodLocal)
}
