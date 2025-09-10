package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.setup.di

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
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.ParachainStakingUnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.ParachainStakingUnbondValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.hints.ParachainStakingUnbondHintsMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.setup.ParachainStakingUnbondViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter

@Module(includes = [ViewModelModule::class])
class ParachainStakingUnbondModule {

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
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
        maxActionProviderFactory: MaxActionProviderFactory,
        delegatorStateUseCase: DelegatorStateUseCase,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
        hintsMixinFactory: ParachainStakingUnbondHintsMixinFactory,
        amountFormatter: AmountFormatter
    ): ViewModel {
        return ParachainStakingUnbondViewModel(
            router = router,
            interactor = interactor,
            addressIconGenerator = addressIconGenerator,
            assetUseCase = assetUseCase,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            validationSystem = validationSystem,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            maxActionProviderFactory = maxActionProviderFactory,
            delegatorStateUseCase = delegatorStateUseCase,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            amountChooserMixinFactory = amountChooserMixinFactory,
            hintsMixinFactory = hintsMixinFactory,
            amountFormatter = amountFormatter
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
