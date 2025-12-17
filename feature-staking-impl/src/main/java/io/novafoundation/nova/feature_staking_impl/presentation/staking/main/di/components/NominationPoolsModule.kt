package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.components

import dagger.Module
import dagger.Provides
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
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.common.PoolDisplayFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.nominationPools.NominationPoolsAlertsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.nominationPools.NominationPoolsNetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.nominationPools.NominationPoolsStakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.nominationPools.NominationPoolsStakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.nominationPools.NominationPoolsUnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.nominationPools.NominationPoolUserRewardsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool.nominationPools.NominationPoolsYourPoolComponentFactory
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter

@Module
class NominationPoolsModule {

    @Provides
    @ScreenScope
    fun provideNetworkInfoComponentFactory(
        parachainNetworkInfoInteractor: NominationPoolsNetworkInfoInteractor,
        resourceManager: ResourceManager,
        amountFormatter: AmountFormatter
    ) = NominationPoolsNetworkInfoComponentFactory(
        resourceManager = resourceManager,
        interactor = parachainNetworkInfoInteractor,
        amountFormatter = amountFormatter
    )

    @Provides
    @ScreenScope
    fun provideStakeSummaryComponentFactory(
        interactor: NominationPoolStakeSummaryInteractor,
        sharedComputation: NominationPoolSharedComputation,
        amountFormatter: AmountFormatter
    ) = NominationPoolsStakeSummaryComponentFactory(
        nominationPoolSharedComputation = sharedComputation,
        interactor = interactor,
        amountFormatter = amountFormatter
    )

    @Provides
    @ScreenScope
    fun provideUnbondComponentFactory(
        interactor: NominationPoolUnbondingsInteractor,
        sharedComputation: NominationPoolSharedComputation,
        router: NominationPoolsRouter,
        amountFormatter: AmountFormatter
    ) = NominationPoolsUnbondingComponentFactory(
        nominationPoolSharedComputation = sharedComputation,
        interactor = interactor,
        router = router,
        amountFormatter = amountFormatter
    )

    @Provides
    @ScreenScope
    fun provideUserRewardsComponentFactory(
        nominationPoolSharedComputation: NominationPoolSharedComputation,
        interactor: NominationPoolsUserRewardsInteractor,
        rewardPeriodsInteractor: StakingRewardPeriodInteractor,
        resourceManager: ResourceManager,
        router: NominationPoolsRouter,
        amountFormatter: AmountFormatter
    ) = NominationPoolUserRewardsComponentFactory(
        nominationPoolSharedComputation = nominationPoolSharedComputation,
        interactor = interactor,
        rewardPeriodsInteractor = rewardPeriodsInteractor,
        resourceManager = resourceManager,
        router = router,
        amountFormatter = amountFormatter
    )

    @Provides
    @ScreenScope
    fun provideYourPoolComponentFactory(
        nominationPoolSharedComputation: NominationPoolSharedComputation,
        interactor: NominationPoolYourPoolInteractor,
        resourceManager: ResourceManager,
        poolDisplayFormatter: PoolDisplayFormatter,
    ) = NominationPoolsYourPoolComponentFactory(
        nominationPoolSharedComputation = nominationPoolSharedComputation,
        interactor = interactor,
        resourceManager = resourceManager,
        poolDisplayFormatter = poolDisplayFormatter
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
        amountFormatter: AmountFormatter
    ) = NominationPoolsAlertsComponentFactory(
        nominationPoolSharedComputation = nominationPoolSharedComputation,
        interactor = interactor,
        resourceManager = resourceManager,
        router = router,
        amountFormatter = amountFormatter
    )
}
