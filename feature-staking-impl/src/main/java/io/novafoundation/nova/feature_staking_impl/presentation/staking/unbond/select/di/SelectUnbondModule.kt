package io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.select.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.UnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.UnbondValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.hints.UnbondHintsMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.select.SelectUnbondViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory

@Module(includes = [ViewModelModule::class])
class SelectUnbondModule {

    @Provides
    @IntoMap
    @ViewModelKey(SelectUnbondViewModel::class)
    fun provideViewModel(
        interactor: StakingInteractor,
        router: StakingRouter,
        unbondInteractor: UnbondInteractor,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        validationSystem: UnbondValidationSystem,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
        maxActionProviderFactory: MaxActionProviderFactory,
        unbondHintsMixinFactory: UnbondHintsMixinFactory,
        amountChooserMixinFactory: AmountChooserMixin.Factory
    ): ViewModel {
        return SelectUnbondViewModel(
            router = router,
            interactor = interactor,
            unbondInteractor = unbondInteractor,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            validationSystem = validationSystem,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            maxActionProviderFactory = maxActionProviderFactory,
            unbondHintsMixinFactory = unbondHintsMixinFactory,
            amountChooserMixinFactory = amountChooserMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SelectUnbondViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectUnbondViewModel::class.java)
    }
}
