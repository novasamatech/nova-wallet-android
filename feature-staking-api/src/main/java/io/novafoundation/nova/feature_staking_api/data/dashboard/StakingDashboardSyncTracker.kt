package io.novafoundation.nova.feature_staking_api.data.dashboard

import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.SyncingStage
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import kotlinx.coroutines.flow.Flow

interface StakingDashboardSyncTracker {

    val syncedItemsFlow: Flow<SyncingStageMap>
}

typealias SyncingStageMap = Map<StakingOptionId, SyncingStage>

fun SyncingStageMap.getSyncingStage(stakingOptionId: StakingOptionId): SyncingStage {
    return get(stakingOptionId) ?: SyncingStage.SYNCING_ALL
}
