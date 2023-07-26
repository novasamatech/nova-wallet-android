package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.StakingDashboardAccountsView
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface StakingDashboardDao {

    @Query(
        """
        SELECT * FROM staking_dashboard_items WHERE
            metaId = :metaId
            AND chainId = :chainId
            AND chainAssetId = :chainAssetId
            AND stakingType = :stakingType
        """
    )
    suspend fun getDashboardItem(
        chainId: String,
        chainAssetId: Int,
        stakingType: String,
        metaId: Long,
    ): StakingDashboardItemLocal?

    @Query("SELECT * FROM staking_dashboard_items WHERE metaId = :metaId")
    fun dashboardItemsFlow(metaId: Long): Flow<List<StakingDashboardItemLocal>>

    @Query("SELECT chainId, chainAssetId, stakingType, stakeStatusAccount, rewardsAccount FROM staking_dashboard_items WHERE metaId = :metaId")
    fun stakingAccountsViewFlow(metaId: Long): Flow<List<StakingDashboardAccountsView>>

    @Query(
        """
        SELECT estimatedEarnings FROM staking_dashboard_items WHERE 
            metaId = :metaId 
            AND chainId = :chainId 
            AND chainAssetId = :chainAssetId 
            AND stakingType = :stakingType
        """
    )
    fun estimatedEarning(metaId: Long, chainId: String, chainAssetId: Int, stakingType: String): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(dashboardItemLocal: StakingDashboardItemLocal)
}
