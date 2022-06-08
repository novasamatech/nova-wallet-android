package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.components

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.alerts.AlertsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.rewards.RewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.UnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_BOND_MORE
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
        stakingInteractor: StakingInteractor,
        alertsInteractor: AlertsInteractor,
        resourceManager: ResourceManager,
        @Named(SYSTEM_MANAGE_STAKING_REDEEM) redeemValidationSystem: StakeActionsValidationSystem,
        @Named(SYSTEM_MANAGE_STAKING_BOND_MORE) bondMoreValidationSystem: StakeActionsValidationSystem,
        router: StakingRouter,
    ) = RelaychainAlertsComponentFactory(
        stakingInteractor = stakingInteractor,
        alertsInteractor = alertsInteractor,
        resourceManager = resourceManager,
        redeemValidationSystem = redeemValidationSystem,
        bondMoreValidationSystem = bondMoreValidationSystem,
        router = router
    )

    @Provides
    @ScreenScope
    fun provideRelaychainNetworkInfoComponentFactory(
        stakingInteractor: StakingInteractor,
        resourceManager: ResourceManager,
    ) = RelaychainNetworkInfoComponentFactory(
        stakingInteractor = stakingInteractor,
        resourceManager = resourceManager
    )

    @Provides
    @ScreenScope
    fun provideRelaychainStakeActionsComponentFactory(
        stakingInteractor: StakingInteractor,
        resourceManager: ResourceManager,
        stakeActionsValidations: Map<@JvmSuppressWildcards String, StakeActionsValidationSystem>,
        router: StakingRouter,
    ) = RelaychainStakeActionsComponentFactory(
        stakingInteractor = stakingInteractor,
        resourceManager = resourceManager,
        stakeActionsValidations = stakeActionsValidations,
        router = router
    )

    @Provides
    @ScreenScope
    fun provideRelaychainStakeSummaryComponentFactory(
        stakingInteractor: StakingInteractor,
        resourceManager: ResourceManager,
    ) = RelaychainStakeSummaryComponentFactory(
        stakingInteractor = stakingInteractor,
        resourceManager = resourceManager,
    )

    @Provides
    @ScreenScope
    fun provideRelaychainStartStakingComponentFactory(
        stakingInteractor: StakingInteractor,
        setupStakingSharedState: SetupStakingSharedState,
        rewardCalculatorFactory: RewardCalculatorFactory,
        resourceManager: ResourceManager,
        router: StakingRouter,
        validationSystem: WelcomeStakingValidationSystem,
        validationExecutor: ValidationExecutor,
    ) = RelaychainStartStakingComponentFactory(
        stakingInteractor = stakingInteractor,
        setupStakingSharedState = setupStakingSharedState,
        rewardCalculatorFactory = rewardCalculatorFactory,
        resourceManager = resourceManager,
        router = router,
        validationSystem = validationSystem,
        validationExecutor = validationExecutor
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
        stakingInteractor: StakingInteractor,
    ) = RelaychainUnbondingComponentFactory(
        unbondInteractor = unbondInteractor,
        validationExecutor = validationExecutor,
        resourceManager = resourceManager,
        rebondValidationSystem = rebondValidationSystem,
        redeemValidationSystem = redeemValidationSystem,
        router = router,
        stakingInteractor = stakingInteractor
    )

    @Provides
    @ScreenScope
    fun provideRelaychainUserRewardsComponentFactory(
        stakingInteractor: StakingInteractor,
    ) = RelaychainUserRewardsComponentFactory(
        stakingInteractor = stakingInteractor
    )
}
