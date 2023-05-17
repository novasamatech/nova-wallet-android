package io.novafoundation.nova.feature_staking_api.data.dashboard;

import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import kotlinx.coroutines.flow.Flow

interface StakingDashboardSyncTracker {

    val syncedItemsFlow: Flow<Set<StakingOptionId>>
}
