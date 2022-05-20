package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.components

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.CompoundStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.relaychain.RelaychainAlertsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.NetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.parachain.ParachainNetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.relaychain.RelaychainNetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.parachain.ParachainStakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.relaychain.RelaychainStakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.parachain.ParachainStakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.relaychain.RelaychainStakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.StartStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.parachain.ParachainStartStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.relaychain.RelaychainStartStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.parachain.ParachainUnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.relaychain.RelaychainUnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.parachain.ParachainUserRewardsComponentFactory
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
        parachainComponentFactory: ParachainNetworkInfoComponentFactory,
        compoundStakingComponentFactory: CompoundStakingComponentFactory,
    ) = NetworkInfoComponentFactory(relaychainComponentFactory, parachainComponentFactory, compoundStakingComponentFactory)

    @Provides
    @ScreenScope
    fun provideStakeActionsComponentFactory(
        relaychainComponentFactory: RelaychainStakeActionsComponentFactory,
        parachainComponentFactory: ParachainStakeActionsComponentFactory,
        compoundStakingComponentFactory: CompoundStakingComponentFactory,
    ) = StakeActionsComponentFactory(relaychainComponentFactory, parachainComponentFactory, compoundStakingComponentFactory)

    @Provides
    @ScreenScope
    fun provideStakeSummaryComponentFactory(
        relaychainComponentFactory: RelaychainStakeSummaryComponentFactory,
        parachainComponentFactory: ParachainStakeSummaryComponentFactory,
        compoundStakingComponentFactory: CompoundStakingComponentFactory,
    ) = StakeSummaryComponentFactory(relaychainComponentFactory, parachainComponentFactory, compoundStakingComponentFactory)

    @Provides
    @ScreenScope
    fun provideStartStakingComponentFactory(
        relaychainComponentFactory: RelaychainStartStakingComponentFactory,
        parachainComponentFactory: ParachainStartStakingComponentFactory,
        compoundStakingComponentFactory: CompoundStakingComponentFactory,
    ) = StartStakingComponentFactory(relaychainComponentFactory, parachainComponentFactory, compoundStakingComponentFactory)

    @Provides
    @ScreenScope
    fun provideUnbondingComponentFactory(
        relaychainComponentFactory: RelaychainUnbondingComponentFactory,
        parachainComponentFactory: ParachainUnbondingComponentFactory,
        compoundStakingComponentFactory: CompoundStakingComponentFactory,
    ) = UnbondingComponentFactory(relaychainComponentFactory, parachainComponentFactory, compoundStakingComponentFactory)

    @Provides
    @ScreenScope
    fun provideUserRewardsComponentFactory(
        relaychainComponentFactory: RelaychainUserRewardsComponentFactory,
        parachainComponentFactory: ParachainUserRewardsComponentFactory,
        compoundStakingComponentFactory: CompoundStakingComponentFactory,
    ) = UserRewardsComponentFactory(relaychainComponentFactory, parachainComponentFactory, compoundStakingComponentFactory)
}
