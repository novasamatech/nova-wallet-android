package io.novafoundation.nova.feature_staking_api.di

import io.novafoundation.nova.feature_staking_api.data.dashboard.StakingDashboardUpdateSystem
import io.novafoundation.nova.feature_staking_api.data.network.blockhain.updaters.PooledBalanceUpdaterFactory
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.TuringAutomationTasksRepository
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.dashboard.StakingDashboardInteractor

interface StakingFeatureApi {

    fun repository(): StakingRepository

    val turingAutomationRepository: TuringAutomationTasksRepository

    val dashboardInteractor: StakingDashboardInteractor

    val dashboardUpdateSystem: StakingDashboardUpdateSystem

    val pooledBalanceUpdaterFactory: PooledBalanceUpdaterFactory
}
