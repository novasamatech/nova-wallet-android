package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.components

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.CompoundStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.relaychain.RelaychainAlertsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.NetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.relaychain.RelaychainNetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.relaychain.RelaychainStakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.relaychain.RelaychainStakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.StartStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.relaychain.RelaychainStartStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.relaychain.RelaychainUnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.relaychain.RelaychainUserRewardsComponentFactory

@Module(includes = [RelaychainModule::class, ParachainModule::class])
class ComponentsModule {

    @Provides
    @ScreenScope
    fun provideAlertsComponentFactory(
        relaychainComponentFactory: RelaychainAlertsComponentFactory,
        compoundStakingComponentFactory: CompoundStakingComponentFactory,
    ) = AlertsComponentFactory(relaychainComponentFactory, compoundStakingComponentFactory)

    @Provides
    @ScreenScope
    fun provideNetworkInfoFactory(
        relaychainComponentFactory: RelaychainNetworkInfoComponentFactory,
        compoundStakingComponentFactory: CompoundStakingComponentFactory,
    ) = NetworkInfoComponentFactory(relaychainComponentFactory, compoundStakingComponentFactory)

    @Provides
    @ScreenScope
    fun provideStakeActionsComponentFactory(
        relaychainComponentFactory: RelaychainStakeActionsComponentFactory,
        compoundStakingComponentFactory: CompoundStakingComponentFactory,
    ) = StakeActionsComponentFactory(relaychainComponentFactory, compoundStakingComponentFactory)

    @Provides
    @ScreenScope
    fun provideStakeSummaryComponentFactory(
        relaychainComponentFactory: RelaychainStakeSummaryComponentFactory,
        compoundStakingComponentFactory: CompoundStakingComponentFactory,
    ) = StakeSummaryComponentFactory(relaychainComponentFactory, compoundStakingComponentFactory)

    @Provides
    @ScreenScope
    fun provideStartStakingComponentFactory(
        relaychainComponentFactory: RelaychainStartStakingComponentFactory,
        compoundStakingComponentFactory: CompoundStakingComponentFactory,
    ) = StartStakingComponentFactory(relaychainComponentFactory, compoundStakingComponentFactory)

    @Provides
    @ScreenScope
    fun provideUnbondingComponentFactory(
        relaychainComponentFactory: RelaychainUnbondingComponentFactory,
        compoundStakingComponentFactory: CompoundStakingComponentFactory,
    ) = UnbondingComponentFactory(relaychainComponentFactory, compoundStakingComponentFactory)

    @Provides
    @ScreenScope
    fun provideUserRewardsComponentFactory(
        relaychainComponentFactory: RelaychainUserRewardsComponentFactory,
        compoundStakingComponentFactory: CompoundStakingComponentFactory,
    ) = UserRewardsComponentFactory(relaychainComponentFactory, compoundStakingComponentFactory)
}
