package io.novafoundation.nova.feature_staking_api.di

import io.novafoundation.nova.feature_staking_api.data.dashboard.StakingDashboardUpdateSystem
import io.novafoundation.nova.feature_staking_api.data.mythos.MythosMainPotMatcherFactory
import io.novafoundation.nova.feature_staking_api.data.network.blockhain.updaters.PooledBalanceUpdaterFactory
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.TuringAutomationTasksRepository
import io.novafoundation.nova.feature_staking_api.di.deeplinks.StakingDeepLinks
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.dashboard.StakingDashboardInteractor
import io.novafoundation.nova.feature_staking_api.presentation.nominationPools.display.PoolDisplayUseCase

interface StakingFeatureApi {

    fun repository(): StakingRepository

    val turingAutomationRepository: TuringAutomationTasksRepository

    val dashboardInteractor: StakingDashboardInteractor

    val dashboardUpdateSystem: StakingDashboardUpdateSystem

    val pooledBalanceUpdaterFactory: PooledBalanceUpdaterFactory

    val poolDisplayUseCase: PoolDisplayUseCase

    val poolAccountDerivation: PoolAccountDerivation

    val mythosMainPotMatcherFactory: MythosMainPotMatcherFactory

    val stakingDeepLinks: StakingDeepLinks
}
