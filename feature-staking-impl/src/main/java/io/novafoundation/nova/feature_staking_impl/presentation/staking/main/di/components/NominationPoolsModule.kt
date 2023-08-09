package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.components

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.alerts.NominationPoolsAlertsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.networkInfo.NominationPoolsNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.stakeSummary.NominationPoolStakeSummaryInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.unbondings.NominationPoolUnbondingsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.userRewards.NominationPoolsUserRewardsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.yourPool.NominationPoolYourPoolInteractor
import io.novafoundation.nova.feature_staking_impl.domain.period.StakingRewardPeriodInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.nominationPools.NominationPoolsAlertsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.nominationPools.NominationPoolsNetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.nominationPools.NominationPoolsStakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.nominationPools.NominationPoolsStakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.nominationPools.NominationPoolsUnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.nominationPools.NominationPoolUserRewardsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool.nominationPools.NominationPoolsYourPoolComponentFactory

@Module
class NominationPoolsModule {

    @Provides
    @ScreenScope
    fun provideNetworkInfoComponentFactory(
        parachainNetworkInfoInteractor: NominationPoolsNetworkInfoInteractor,
        resourceManager: ResourceManager,
    ) = NominationPoolsNetworkInfoComponentFactory(
        resourceManager = resourceManager,
        interactor = parachainNetworkInfoInteractor,
    )

    @Provides
    @ScreenScope
    fun provideStakeSummaryComponentFactory(
        interactor: NominationPoolStakeSummaryInteractor,
        sharedComputation: NominationPoolSharedComputation,
    ) = NominationPoolsStakeSummaryComponentFactory(
        nominationPoolSharedComputation = sharedComputation,
        interactor = interactor
    )

    @Provides
    @ScreenScope
    fun provideUnbondComponentFactory(
        interactor: NominationPoolUnbondingsInteractor,
        sharedComputation: NominationPoolSharedComputation,
        router: NominationPoolsRouter,
    ) = NominationPoolsUnbondingComponentFactory(
        nominationPoolSharedComputation = sharedComputation,
        interactor = interactor,
        router = router
    )

    @Provides
    @ScreenScope
    fun provideUserRewardsComponentFactory(
        nominationPoolSharedComputation: NominationPoolSharedComputation,
        interactor: NominationPoolsUserRewardsInteractor,
        rewardPeriodsInteractor: StakingRewardPeriodInteractor,
        resourceManager: ResourceManager
    ) = NominationPoolUserRewardsComponentFactory(
        nominationPoolSharedComputation = nominationPoolSharedComputation,
        interactor = interactor,
        rewardPeriodsInteractor = rewardPeriodsInteractor,
        resourceManager = resourceManager
    )

    @Provides
    @ScreenScope
    fun provideYourPoolComponentFactory(
        nominationPoolSharedComputation: NominationPoolSharedComputation,
        interactor: NominationPoolYourPoolInteractor,
        addressIconGenerator: AddressIconGenerator,
        resourceManager: ResourceManager,
    ) = NominationPoolsYourPoolComponentFactory(
        nominationPoolSharedComputation = nominationPoolSharedComputation,
        interactor = interactor,
        addressIconGenerator = addressIconGenerator,
        resourceManager = resourceManager
    )

    @Provides
    @ScreenScope
    fun provideStakeActionsComponentFactory(
        resourceManager: ResourceManager,
        sharedComputation: NominationPoolSharedComputation,
        router: NominationPoolsRouter
    ) = NominationPoolsStakeActionsComponentFactory(
        nominationPoolSharedComputation = sharedComputation,
        resourceManager = resourceManager,
        router = router
    )

    @Provides
    @ScreenScope
    fun provideAlertsComponentFactory(
        nominationPoolSharedComputation: NominationPoolSharedComputation,
        interactor: NominationPoolsAlertsInteractor,
        resourceManager: ResourceManager,
        router: NominationPoolsRouter,
    ) = NominationPoolsAlertsComponentFactory(
        nominationPoolSharedComputation = nominationPoolSharedComputation,
        interactor = interactor,
        resourceManager = resourceManager,
        router = router
    )
}
