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
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.alerts.AlertsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.rewards.RewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.validations.balance.ManageStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.validations.balance.SYSTEM_MANAGE_STAKING_BOND_MORE
import io.novafoundation.nova.feature_staking_impl.domain.validations.balance.SYSTEM_MANAGE_STAKING_REDEEM
import io.novafoundation.nova.feature_staking_impl.domain.validations.welcome.WelcomeStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.StakingViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorMixin
import javax.inject.Named

@Module(includes = [ViewModelModule::class])
class StakingModule {

    @Provides
    @ScreenScope
    fun provideStakingViewStateFactory(
        interactor: StakingInteractor,
        setupStakingSharedState: SetupStakingSharedState,
        resourceManager: ResourceManager,
        rewardCalculatorFactory: RewardCalculatorFactory,
        router: StakingRouter,
        welcomeStakingValidationSystem: WelcomeStakingValidationSystem,
        validationExecutor: ValidationExecutor
    ) = StakingViewStateFactory(
        interactor,
        setupStakingSharedState,
        resourceManager,
        router,
        rewardCalculatorFactory,
        welcomeStakingValidationSystem,
        validationExecutor
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
        @Named(SYSTEM_MANAGE_STAKING_REDEEM) redeemValidationSystem: ManageStakingValidationSystem,
        @Named(SYSTEM_MANAGE_STAKING_BOND_MORE) bondMoreValidationSystem: ManageStakingValidationSystem,
        validationExecutor: ValidationExecutor,
        stakingUpdateSystem: UpdateSystem,
        assetSelectorFactory: MixinFactory<AssetSelectorMixin.Presentation>,
        selectedAssetState: StakingSharedState
    ): ViewModel {
        return StakingViewModel(
            interactor,
            alertsInteractor,
            addressIconGenerator,
            stakingViewStateFactory,
            router,
            resourceManager,
            redeemValidationSystem,
            bondMoreValidationSystem,
            validationExecutor,
            stakingUpdateSystem,
            assetSelectorFactory,
            selectedAssetState,
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
