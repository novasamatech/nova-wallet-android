package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId

class StakingDashboardOptionUpdated(val option: StakingOptionId) : Updater.SideEffect
