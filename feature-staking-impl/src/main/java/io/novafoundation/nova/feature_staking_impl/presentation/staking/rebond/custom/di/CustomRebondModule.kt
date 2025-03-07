package io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.custom.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.rebond.RebondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.rebond.RebondValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.custom.CustomRebondViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory

@Module(includes = [ViewModelModule::class])
class CustomRebondModule {

    @Provides
    @IntoMap
    @ViewModelKey(CustomRebondViewModel::class)
    fun provideViewModel(
        interactor: StakingInteractor,
        router: StakingRouter,
        rebondInteractor: RebondInteractor,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
        maxActionProviderFactory: MaxActionProviderFactory,
        validationSystem: RebondValidationSystem,
        amountChooserMixin: AmountChooserMixin.Factory,
        hintsMixinFactory: ResourcesHintsMixinFactory,
    ): ViewModel {
        return CustomRebondViewModel(
            router = router,
            interactor = interactor,
            rebondInteractor = rebondInteractor,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            validationSystem = validationSystem,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            maxActionProviderFactory = maxActionProviderFactory,
            amountChooserMixinFactory = amountChooserMixin,
            hintsMixinFactory = hintsMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): CustomRebondViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(CustomRebondViewModel::class.java)
    }
}
