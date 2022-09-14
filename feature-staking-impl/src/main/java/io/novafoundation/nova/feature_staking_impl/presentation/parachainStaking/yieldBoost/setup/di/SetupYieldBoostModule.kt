package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.setup.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.validations.YieldBoostValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.setup.SetupYieldBoostViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class SetupYieldBoostModule {

    @Provides
    @IntoMap
    @ViewModelKey(SetupYieldBoostViewModel::class)
    fun provideViewModel(
        router: ParachainStakingRouter,
        interactor: YieldBoostInteractor,
        addressIconGenerator: AddressIconGenerator,
        assetUseCase: AssetUseCase,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        feeLoaderMixin: FeeLoaderMixin.Presentation,
        delegatorStateUseCase: DelegatorStateUseCase,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
        collatorsUseCase: CollatorsUseCase,
        yieldBoostValidationSystem: YieldBoostValidationSystem,
    ): ViewModel {
        return SetupYieldBoostViewModel(
            router = router,
            interactor = interactor,
            addressIconGenerator = addressIconGenerator,
            assetUseCase = assetUseCase,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            feeLoaderMixin = feeLoaderMixin,
            delegatorStateUseCase = delegatorStateUseCase,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            collatorsUseCase = collatorsUseCase,
            amountChooserMixinFactory = amountChooserMixinFactory,
            validationSystem = yieldBoostValidationSystem
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SetupYieldBoostViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SetupYieldBoostViewModel::class.java)
    }
}
