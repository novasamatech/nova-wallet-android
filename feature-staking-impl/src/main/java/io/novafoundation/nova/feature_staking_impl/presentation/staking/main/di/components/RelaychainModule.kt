package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.components

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.alerts.AlertsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bagList.rebag.validations.RebagValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.period.StakingRewardPeriodInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.UnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_BOND_MORE
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_REBAG
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_REBOND
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_REDEEM
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.validations.welcome.WelcomeStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.relaychain.RelaychainAlertsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.relaychain.RelaychainNetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.relaychain.RelaychainStakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.relaychain.RelaychainStakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.relaychain.RelaychainStartStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.relaychain.RelaychainUnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.relaychain.RelaychainUserRewardsComponentFactory
import javax.inject.Named

@Module
class RelaychainModule {

    @Provides
    @ScreenScope
    fun provideRelaychainAlertsComponentFactory(
        stakingSharedComputation: StakingSharedComputation,
        alertsInteractor: AlertsInteractor,
        resourceManager: ResourceManager,
        @Named(SYSTEM_MANAGE_STAKING_REDEEM) redeemValidationSystem: StakeActionsValidationSystem,
        @Named(SYSTEM_MANAGE_STAKING_BOND_MORE) bondMoreValidationSystem: StakeActionsValidationSystem,
        @Named(SYSTEM_MANAGE_STAKING_REBAG) rebagValidationSystem: StakeActionsValidationSystem,
        router: StakingRouter,
    ) = RelaychainAlertsComponentFactory(
        stakingSharedComputation = stakingSharedComputation,
        alertsInteractor = alertsInteractor,
        resourceManager = resourceManager,
        redeemValidationSystem = redeemValidationSystem,
        bondMoreValidationSystem = bondMoreValidationSystem,
        rebagValidationSystem = rebagValidationSystem,
        router = router
    )

    @Provides
    @ScreenScope
    fun provideRelaychainNetworkInfoComponentFactory(
        stakingInteractor: StakingInteractor,
        resourceManager: ResourceManager,
        stakingSharedComputation: StakingSharedComputation,
    ) = RelaychainNetworkInfoComponentFactory(
        stakingInteractor = stakingInteractor,
        resourceManager = resourceManager,
        stakingSharedComputation = stakingSharedComputation,
    )

    @Provides
    @ScreenScope
    fun provideRelaychainStakeActionsComponentFactory(
        stakingSharedComputation: StakingSharedComputation,
        resourceManager: ResourceManager,
        stakeActionsValidations: Map<@JvmSuppressWildcards String, StakeActionsValidationSystem>,
        router: StakingRouter,
    ) = RelaychainStakeActionsComponentFactory(
        stakingSharedComputation = stakingSharedComputation,
        resourceManager = resourceManager,
        stakeActionsValidations = stakeActionsValidations,
        router = router
    )

    @Provides
    @ScreenScope
    fun provideRelaychainStakeSummaryComponentFactory(
        stakingInteractor: StakingInteractor,
        resourceManager: ResourceManager,
        stakingSharedComputation: StakingSharedComputation,
    ) = RelaychainStakeSummaryComponentFactory(
        stakingInteractor = stakingInteractor,
        resourceManager = resourceManager,
        stakingSharedComputation = stakingSharedComputation,
    )

    @Provides
    @ScreenScope
    fun provideRelaychainStartStakingComponentFactory(
        stakingSharedComputation: StakingSharedComputation,
        setupStakingSharedState: SetupStakingSharedState,
        resourceManager: ResourceManager,
        router: StakingRouter,
        validationSystem: WelcomeStakingValidationSystem,
        validationExecutor: ValidationExecutor,
    ) = RelaychainStartStakingComponentFactory(
        setupStakingSharedState = setupStakingSharedState,
        resourceManager = resourceManager,
        router = router,
        validationSystem = validationSystem,
        validationExecutor = validationExecutor,
        stakingSharedComputation = stakingSharedComputation
    )

    @Provides
    @ScreenScope
    fun provideRelaychainUnbondingComponentFactory(
        unbondInteractor: UnbondInteractor,
        validationExecutor: ValidationExecutor,
        resourceManager: ResourceManager,
        @Named(SYSTEM_MANAGE_STAKING_REBOND) rebondValidationSystem: StakeActionsValidationSystem,
        @Named(SYSTEM_MANAGE_STAKING_REDEEM) redeemValidationSystem: StakeActionsValidationSystem,
        router: StakingRouter,
        stakingSharedComputation: StakingSharedComputation,
    ) = RelaychainUnbondingComponentFactory(
        unbondInteractor = unbondInteractor,
        validationExecutor = validationExecutor,
        resourceManager = resourceManager,
        rebondValidationSystem = rebondValidationSystem,
        redeemValidationSystem = redeemValidationSystem,
        router = router,
        stakingSharedComputation = stakingSharedComputation,
    )

    @Provides
    @ScreenScope
    fun provideRelaychainUserRewardsComponentFactory(
        stakingInteractor: StakingInteractor,
        stakingSharedComputation: StakingSharedComputation,
        stakingRewardPeriodInteractor: StakingRewardPeriodInteractor,
        resourceManager: ResourceManager
    ) = RelaychainUserRewardsComponentFactory(
        stakingInteractor = stakingInteractor,
        stakingSharedComputation = stakingSharedComputation,
        rewardPeriodsInteractor = stakingRewardPeriodInteractor,
        resourceManager = resourceManager
    )
}
