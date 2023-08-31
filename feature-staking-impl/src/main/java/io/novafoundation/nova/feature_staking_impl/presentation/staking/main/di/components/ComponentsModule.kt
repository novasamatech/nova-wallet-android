package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.components

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.CompoundStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.nominationPools.NominationPoolsAlertsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.parachain.ParachainAlertsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.relaychain.RelaychainAlertsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.NetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.nominationPools.NominationPoolsNetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.parachain.ParachainNetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.relaychain.RelaychainNetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.nominationPools.NominationPoolsStakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.parachain.ParachainStakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.parachain.turing.TuringStakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.relaychain.RelaychainStakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.nominationPools.NominationPoolsStakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.parachain.ParachainStakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.relaychain.RelaychainStakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.nominationPools.NominationPoolsUnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.parachain.ParachainUnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.relaychain.RelaychainUnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.nominationPools.NominationPoolUserRewardsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.parachain.ParachainUserRewardsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.relaychain.RelaychainUserRewardsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool.YourPoolComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool.nominationPools.NominationPoolsYourPoolComponentFactory

@Module(includes = [RelaychainModule::class, ParachainModule::class, TuringModule::class, NominationPoolsModule::class])
class ComponentsModule {

    @Provides
    @ScreenScope
    fun provideAlertsComponentFactory(
        relaychainComponentFactory: RelaychainAlertsComponentFactory,
        parachainComponentFactory: ParachainAlertsComponentFactory,
        nominationPoolsFactory: NominationPoolsAlertsComponentFactory,
        compoundStakingComponentFactory: CompoundStakingComponentFactory,
    ) = AlertsComponentFactory(relaychainComponentFactory, parachainComponentFactory, nominationPoolsFactory, compoundStakingComponentFactory)

    @Provides
    @ScreenScope
    fun provideNetworkInfoFactory(
        relaychainComponentFactory: RelaychainNetworkInfoComponentFactory,
        parachainComponentFactory: ParachainNetworkInfoComponentFactory,
        nominationPoolsNetworkInfoComponentFactory: NominationPoolsNetworkInfoComponentFactory,
        compoundStakingComponentFactory: CompoundStakingComponentFactory,
    ) = NetworkInfoComponentFactory(
        relaychainComponentFactory = relaychainComponentFactory,
        parachainComponentFactory = parachainComponentFactory,
        nominationPoolsComponentFactory = nominationPoolsNetworkInfoComponentFactory,
        compoundStakingComponentFactory = compoundStakingComponentFactory
    )

    @Provides
    @ScreenScope
    fun provideStakeActionsComponentFactory(
        relaychainComponentFactory: RelaychainStakeActionsComponentFactory,
        parachainComponentFactory: ParachainStakeActionsComponentFactory,
        turingStakeActionsComponentFactory: TuringStakeActionsComponentFactory,
        nominationPoolsStakeActionsComponentFactory: NominationPoolsStakeActionsComponentFactory,
        compoundStakingComponentFactory: CompoundStakingComponentFactory,
    ) = StakeActionsComponentFactory(
        relaychainComponentFactory = relaychainComponentFactory,
        parachainComponentFactory = parachainComponentFactory,
        turingComponentFactory = turingStakeActionsComponentFactory,
        nominationPoolsComponentFactory = nominationPoolsStakeActionsComponentFactory,
        compoundStakingComponentFactory = compoundStakingComponentFactory
    )

    @Provides
    @ScreenScope
    fun provideStakeSummaryComponentFactory(
        relaychainComponentFactory: RelaychainStakeSummaryComponentFactory,
        parachainComponentFactory: ParachainStakeSummaryComponentFactory,
        nominationPoolsStakeSummaryComponentFactory: NominationPoolsStakeSummaryComponentFactory,
        compoundStakingComponentFactory: CompoundStakingComponentFactory,
    ) = StakeSummaryComponentFactory(
        relaychainComponentFactory = relaychainComponentFactory,
        parachainStakeSummaryComponentFactory = parachainComponentFactory,
        nominationPoolsStakeSummaryComponentFactory = nominationPoolsStakeSummaryComponentFactory,
        compoundStakingComponentFactory = compoundStakingComponentFactory
    )

    @Provides
    @ScreenScope
    fun provideUnbondingComponentFactory(
        relaychainComponentFactory: RelaychainUnbondingComponentFactory,
        parachainComponentFactory: ParachainUnbondingComponentFactory,
        nominationPoolsUnbondingComponentFactory: NominationPoolsUnbondingComponentFactory,
        compoundStakingComponentFactory: CompoundStakingComponentFactory,
    ) = UnbondingComponentFactory(
        relaychainUnbondingComponentFactory = relaychainComponentFactory,
        parachainComponentFactory = parachainComponentFactory,
        nominationPoolsUnbondingComponentFactory = nominationPoolsUnbondingComponentFactory,
        compoundStakingComponentFactory = compoundStakingComponentFactory
    )

    @Provides
    @ScreenScope
    fun provideUserRewardsComponentFactory(
        relaychainComponentFactory: RelaychainUserRewardsComponentFactory,
        parachainComponentFactory: ParachainUserRewardsComponentFactory,
        nominationPoolUserRewardsComponentFactory: NominationPoolUserRewardsComponentFactory,
        compoundStakingComponentFactory: CompoundStakingComponentFactory,
    ) = UserRewardsComponentFactory(
        relaychainComponentFactory = relaychainComponentFactory,
        parachainComponentFactory = parachainComponentFactory,
        nominationPoolsComponentFactory = nominationPoolUserRewardsComponentFactory,
        compoundStakingComponentFactory = compoundStakingComponentFactory
    )

    @Provides
    @ScreenScope
    fun provideYourPoolComponentFactory(
        nominationPoolsFactory: NominationPoolsYourPoolComponentFactory,
        compoundFactory: CompoundStakingComponentFactory,
    ) = YourPoolComponentFactory(nominationPoolsFactory, compoundFactory)
}
