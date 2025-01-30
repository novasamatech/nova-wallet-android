package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.components

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.main.stakeSummary.MythosStakeSummaryInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.main.unbonding.MythosUnbondingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.main.userRewards.MythosUserRewardsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.period.StakingRewardPeriodInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.mythos.MythosStakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.mythos.MythosStakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.mythos.MythosUnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.mythos.MythosUserRewardsComponentFactory

@Module
class MythosModule {

    @Provides
    @ScreenScope
    fun provideStakeSummaryComponentFactory(
        mythosSharedComputation: MythosSharedComputation,
        interactor: MythosStakeSummaryInteractor,
    ) = MythosStakeSummaryComponentFactory(
        mythosSharedComputation = mythosSharedComputation,
        interactor = interactor
    )

    @Provides
    @ScreenScope
    fun provideStakeActionsFactory(
        mythosSharedComputation: MythosSharedComputation,
        resourceManager: ResourceManager,
        router: MythosStakingRouter,
    ) = MythosStakeActionsComponentFactory(
        mythosSharedComputation = mythosSharedComputation,
        resourceManager = resourceManager,
        router = router
    )

    @Provides
    @ScreenScope
    fun provideUnbondingComponentFactory(
        mythosSharedComputation: MythosSharedComputation,
        interactor: MythosUnbondingInteractor,
        router: MythosStakingRouter,
    ) = MythosUnbondingComponentFactory(
        mythosSharedComputation = mythosSharedComputation,
        interactor = interactor,
        router = router
    )

    @Provides
    @ScreenScope
    fun provideUserRewardsComponentFactory(
        router: MythosStakingRouter,
        mythosSharedComputation: MythosSharedComputation,
        interactor: MythosUserRewardsInteractor,
        rewardPeriodsInteractor: StakingRewardPeriodInteractor,
        resourceManager: ResourceManager,
    ) = MythosUserRewardsComponentFactory(
        router = router,
        mythosSharedComputation = mythosSharedComputation,
        interactor = interactor,
        rewardPeriodsInteractor = rewardPeriodsInteractor,
        resourceManager = resourceManager
    )
}
