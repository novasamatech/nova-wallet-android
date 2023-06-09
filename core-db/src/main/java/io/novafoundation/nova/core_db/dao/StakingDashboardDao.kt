package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.core_db.model.StakingDashboardPrimaryAccountView
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

    @Query("SELECT chainId, chainAssetId, stakingType, primaryStakingAccountId from staking_dashboard_items WHERE metaId = :metaId")
    fun stakingAccountsViewFlow(metaId: Long): Flow<List<StakingDashboardPrimaryAccountView>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(dashboardItemLocal: StakingDashboardItemLocal)
}
