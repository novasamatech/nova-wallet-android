package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.components

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.ParachainNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.alerts.ParachainStakingAlertsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.stakeSummary.ParachainStakingStakeSummaryInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.unbondings.ParachainStakingUnbondingsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.userRewards.ParachainStakingUserRewardsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.preliminary.ParachainStakingUnbondPreliminaryValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.period.StakingRewardPeriodInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.parachain.ParachainAlertsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.parachain.ParachainNetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.parachain.ParachainStakeActionsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.parachain.ParachainStakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.parachain.ParachainUnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.parachain.ParachainUserRewardsComponentFactory

@Module
class ParachainModule {

    @Provides
    @ScreenScope
    fun provideParachainStakeSummaryComponentFactory(
        delegatorStateUseCase: DelegatorStateUseCase,
        interactor: ParachainStakingStakeSummaryInteractor,
        resourceManager: ResourceManager,
    ) = ParachainStakeSummaryComponentFactory(
        delegatorStateUseCase = delegatorStateUseCase,
        resourceManager = resourceManager,
        interactor = interactor
    )

    @Provides
    @ScreenScope
    fun provideParachainNetworkInfoComponentFactory(
        delegatorStateUseCase: DelegatorStateUseCase,
        parachainNetworkInfoInteractor: ParachainNetworkInfoInteractor,
        resourceManager: ResourceManager,
    ) = ParachainNetworkInfoComponentFactory(
        delegatorStateUseCase = delegatorStateUseCase,
        resourceManager = resourceManager,
        interactor = parachainNetworkInfoInteractor,
    )

    @Provides
    @ScreenScope
    fun provideParachainUserRewardsComponentFactory(
        delegatorStateUseCase: DelegatorStateUseCase,
        interactor: ParachainStakingUserRewardsInteractor,
        stakingRewardPeriodInteractor: StakingRewardPeriodInteractor,
        resourceManager: ResourceManager
    ) = ParachainUserRewardsComponentFactory(
        delegatorStateUseCase = delegatorStateUseCase,
        interactor = interactor,
        rewardPeriodsInteractor = stakingRewardPeriodInteractor,
        resourceManager = resourceManager
    )

    @Provides
    @ScreenScope
    fun provideParachainUnbondingsFactory(
        delegatorStateUseCase: DelegatorStateUseCase,
        interactor: ParachainStakingUnbondingsInteractor,
        router: ParachainStakingRouter,
        addressIconGenerator: AddressIconGenerator,
        resourceManager: ResourceManager
    ) = ParachainUnbondingComponentFactory(
        delegatorStateUseCase = delegatorStateUseCase,
        interactor = interactor,
        router = router,
        addressIconGenerator = addressIconGenerator,
        resourceManager = resourceManager
    )

    @Provides
    @ScreenScope
    fun provideStakeActionsFactory(
        delegatorStateUseCase: DelegatorStateUseCase,
        resourceManager: ResourceManager,
        router: ParachainStakingRouter,
        validationExecutor: ValidationExecutor,
        unbondPreliminaryValidationSystem: ParachainStakingUnbondPreliminaryValidationSystem,
    ) = ParachainStakeActionsComponentFactory(
        delegatorStateUseCase = delegatorStateUseCase,
        resourceManager = resourceManager,
        router = router,
        validationExecutor = validationExecutor,
        unbondPreliminaryValidationSystem = unbondPreliminaryValidationSystem
    )

    @Provides
    @ScreenScope
    fun provideAlertsFactory(
        delegatorStateUseCase: DelegatorStateUseCase,
        resourceManager: ResourceManager,
        router: ParachainStakingRouter,
        interactor: ParachainStakingAlertsInteractor
    ) = ParachainAlertsComponentFactory(
        delegatorStateUseCase = delegatorStateUseCase,
        resourceManager = resourceManager,
        router = router,
        interactor = interactor
    )
}
