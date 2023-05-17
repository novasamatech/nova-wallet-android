package io.novafoundation.nova.feature_staking_api.data.dashboard

import io.novafoundation.nova.core.updater.UpdateSystem

interface StakingDashboardUpdateSystem : UpdateSystem, StakingDashboardSyncTracker
