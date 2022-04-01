package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
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
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.StakingViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.manage.ManageStakeAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.manage.ManageStakeMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.unbonding.UnbondingMixinFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorMixin
import javax.inject.Named

@Module(includes = [ViewModelModule::class])
class StakingModule {

    @Provides
    @ScreenScope
    fun provideUnbondingMixinFactory(
        unbondInteractor: UnbondInteractor,
        validationExecutor: ValidationExecutor,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        resourceManager: ResourceManager,
        @Named(SYSTEM_MANAGE_STAKING_REDEEM) redeemValidationSystem: StakeActionsValidationSystem,
        @Named(SYSTEM_MANAGE_STAKING_REBOND) rebondValidationSystem: StakeActionsValidationSystem,
        router: StakingRouter,
    ) = UnbondingMixinFactory(
        unbondInteractor = unbondInteractor,
        validationExecutor = validationExecutor,
        actionAwaitableFactory = actionAwaitableMixinFactory,
        resourceManager = resourceManager,
        rebondValidationSystem = rebondValidationSystem,
        redeemValidationSystem = redeemValidationSystem,
        router = router
    )

    @Provides
    @ScreenScope
    fun provideManageStakingMixinFactory(
        validationExecutor: ValidationExecutor,
        resourceManager: ResourceManager,
        router: StakingRouter,
        stakeActionsValidations: Map<@JvmSuppressWildcards ManageStakeAction, StakeActionsValidationSystem>,
    ) = ManageStakeMixinFactory(
        validationExecutor = validationExecutor,
        resourceManager = resourceManager,
        stakeActionsValidations = stakeActionsValidations,
        router = router
    )

    @Provides
    @ScreenScope
    fun provideStakingViewStateFactory(
        interactor: StakingInteractor,
        setupStakingSharedState: SetupStakingSharedState,
        resourceManager: ResourceManager,
        rewardCalculatorFactory: RewardCalculatorFactory,
        router: StakingRouter,
        welcomeStakingValidationSystem: WelcomeStakingValidationSystem,
        validationExecutor: ValidationExecutor,
        unbondingMixinFactory: UnbondingMixinFactory,
        manageStakeMixinFactory: ManageStakeMixinFactory,
    ) = StakingViewStateFactory(
        stakingInteractor = interactor,
        setupStakingSharedState = setupStakingSharedState,
        resourceManager = resourceManager,
        router = router,
        rewardCalculatorFactory = rewardCalculatorFactory,
        welcomeStakingValidationSystem = welcomeStakingValidationSystem,
        manageStakeMixinFactory = manageStakeMixinFactory,
        unbondingMixinFactory = unbondingMixinFactory,
        validationExecutor = validationExecutor
    )

    @Provides
    @IntoMap
    @ViewModelKey(StakingViewModel::class)
    fun provideViewModel(
        interactor: StakingInteractor,
        alertsInteractor: AlertsInteractor,
        addressIconGenerator: AddressIconGenerator,
        stakingViewStateFactory: StakingViewStateFactory,
        router: StakingRouter,
        resourceManager: ResourceManager,
        @Named(SYSTEM_MANAGE_STAKING_REDEEM) redeemValidationSystem: StakeActionsValidationSystem,
        @Named(SYSTEM_MANAGE_STAKING_BOND_MORE) bondMoreValidationSystem: StakeActionsValidationSystem,
        validationExecutor: ValidationExecutor,
        stakingUpdateSystem: UpdateSystem,
        assetSelectorFactory: MixinFactory<AssetSelectorMixin.Presentation>,
        selectedAssetState: StakingSharedState
    ): ViewModel {
        return StakingViewModel(
            interactor = interactor,
            alertsInteractor = alertsInteractor,
            addressIconGenerator = addressIconGenerator,
            stakingViewStateFactory = stakingViewStateFactory,
            router = router,
            resourceManager = resourceManager,
            redeemValidationSystem = redeemValidationSystem,
            bondMoreValidationSystem = bondMoreValidationSystem,
            validationExecutor = validationExecutor,
            stakingUpdateSystem = stakingUpdateSystem,
            assetSelectorMixinFactory = assetSelectorFactory,
            selectedAssetState = selectedAssetState,
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): StakingViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(StakingViewModel::class.java)
    }
}
