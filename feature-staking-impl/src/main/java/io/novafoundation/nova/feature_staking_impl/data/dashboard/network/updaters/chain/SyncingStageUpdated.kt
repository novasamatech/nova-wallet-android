package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId

sealed class StakingDashboardUpdaterEvent : Updater.SideEffect {

    class AllSynced(val option: StakingOptionId, val indexOfUsedOffChainSync: Int): StakingDashboardUpdaterEvent()

    class PrimarySynced(val option: StakingOptionId) : StakingDashboardUpdaterEvent()
}
