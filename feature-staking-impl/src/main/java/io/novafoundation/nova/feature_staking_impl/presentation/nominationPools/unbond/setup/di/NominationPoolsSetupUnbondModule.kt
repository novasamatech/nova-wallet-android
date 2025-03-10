package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.setup.di

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
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.NominationPoolsUnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond.validations.NominationPoolsUnbondValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.common.di.NominationPoolsCommonUnbondModule
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.hints.NominationPoolsUnbondHintsFactory
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.setup.NominationPoolsSetupUnbondViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory

@Module(includes = [ViewModelModule::class, NominationPoolsCommonUnbondModule::class])
class NominationPoolsSetupUnbondModule {

    @Provides
    @IntoMap
    @ViewModelKey(NominationPoolsSetupUnbondViewModel::class)
    fun provideViewModel(
        router: NominationPoolsRouter,
        interactor: NominationPoolsUnbondInteractor,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        validationSystem: NominationPoolsUnbondValidationSystem,
        stakingSharedState: StakingSharedState,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
        assetUseCase: AssetUseCase,
        hintsFactory: NominationPoolsUnbondHintsFactory,
        maxActionProviderFactory: MaxActionProviderFactory,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
    ): ViewModel {
        return NominationPoolsSetupUnbondViewModel(
            router = router,
            interactor = interactor,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            validationSystem = validationSystem,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            assetUseCase = assetUseCase,
            hintsFactory = hintsFactory,
            amountChooserMixinFactory = amountChooserMixinFactory,
            maxActionProviderFactory = maxActionProviderFactory,
            stakingSharedState = stakingSharedState
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): NominationPoolsSetupUnbondViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(NominationPoolsSetupUnbondViewModel::class.java)
    }
}
