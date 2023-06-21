package io.novafoundation.nova.feature_staking_api.domain.dashboard

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.MoreStakingOptions
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboard
import kotlinx.coroutines.flow.Flow

interface StakingDashboardInteractor {

    suspend fun syncDapps()

    fun stakingDashboardFlow(): Flow<ExtendedLoadingState<StakingDashboard>>

    fun moreStakingOptionsFlow(): Flow<MoreStakingOptions>
}
