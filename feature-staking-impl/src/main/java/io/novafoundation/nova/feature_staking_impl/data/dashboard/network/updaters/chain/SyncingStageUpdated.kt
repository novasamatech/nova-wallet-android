package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.SyncingStage
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import jp.co.soramitsu.fearless_utils.runtime.AccountId

sealed class StakingDashboardUpdaterEvent : Updater.SideEffect {

    class SyncingStageUpdated(val option: StakingOptionId, val syncingStage: SyncingStage) : StakingDashboardUpdaterEvent()

    class PrimaryStakingAccountResolved(val option: StakingOptionId, val primaryAccount: AccountId?) : StakingDashboardUpdaterEvent()
}
