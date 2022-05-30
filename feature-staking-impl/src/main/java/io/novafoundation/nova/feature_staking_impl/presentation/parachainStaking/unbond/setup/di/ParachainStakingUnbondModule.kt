package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.setup.di

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
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.ParachainStakingUnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.ParachainStakingUnbondValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards.RealParachainStakingRewardsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.hints.ParachainStakingUnbondHintsMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.setup.ParachainStakingUnbondViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class ParachainStakingUnbondModule {

    @Provides
    @ScreenScope
    fun provideRewardsComponentFactory(
        rewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
        singleAssetSharedState: StakingSharedState,
        resourceManager: ResourceManager,
    ) = RealParachainStakingRewardsComponentFactory(rewardCalculatorFactory, singleAssetSharedState, resourceManager)

    @Provides
    @IntoMap
    @ViewModelKey(ParachainStakingUnbondViewModel::class)
    fun provideViewModel(
        router: ParachainStakingRouter,
        interactor: ParachainStakingUnbondInteractor,
        addressIconGenerator: AddressIconGenerator,
        assetUseCase: AssetUseCase,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        validationSystem: ParachainStakingUnbondValidationSystem,
        feeLoaderMixin: FeeLoaderMixin.Presentation,
        delegatorStateUseCase: DelegatorStateUseCase,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        collatorsUseCase: CollatorsUseCase,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
        hintsMixinFactory: ParachainStakingUnbondHintsMixinFactory
    ): ViewModel {
        return ParachainStakingUnbondViewModel(
            router = router,
            interactor = interactor,
            addressIconGenerator = addressIconGenerator,
            assetUseCase = assetUseCase,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            validationSystem = validationSystem,
            feeLoaderMixin = feeLoaderMixin,
            delegatorStateUseCase = delegatorStateUseCase,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            collatorsUseCase = collatorsUseCase,
            amountChooserMixinFactory = amountChooserMixinFactory,
            hintsMixinFactory = hintsMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ParachainStakingUnbondViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ParachainStakingUnbondViewModel::class.java)
    }
}
