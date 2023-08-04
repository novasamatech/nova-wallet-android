package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.setup.di

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
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.NominationPoolsBondMoreInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore.validations.NominationPoolsBondMoreValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.common.di.NominationPoolsCommonBondMoreModule
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.hints.NominationPoolsBondMoreHintsFactory
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.setup.NominationPoolsSetupBondMoreViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class, NominationPoolsCommonBondMoreModule::class])
class NominationPoolsSetupBondMoreModule {

    @Provides
    @IntoMap
    @ViewModelKey(NominationPoolsSetupBondMoreViewModel::class)
    fun provideViewModel(
        router: NominationPoolsRouter,
        interactor: NominationPoolsBondMoreInteractor,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        validationSystem: NominationPoolsBondMoreValidationSystem,
        feeLoaderMixin: FeeLoaderMixin.Presentation,
        poolMemberUseCase: NominationPoolMemberUseCase,
        assetUseCase: AssetUseCase,
        hintsFactory: NominationPoolsBondMoreHintsFactory,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
    ): ViewModel {
        return NominationPoolsSetupBondMoreViewModel(
            router = router,
            interactor = interactor,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            validationSystem = validationSystem,
            feeLoaderMixin = feeLoaderMixin,
            poolMemberUseCase = poolMemberUseCase,
            assetUseCase = assetUseCase,
            hintsFactory = hintsFactory,
            amountChooserMixinFactory = amountChooserMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): NominationPoolsSetupBondMoreViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(NominationPoolsSetupBondMoreViewModel::class.java)
    }
}
