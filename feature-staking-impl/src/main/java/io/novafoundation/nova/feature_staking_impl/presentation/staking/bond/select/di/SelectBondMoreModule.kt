package io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.select.di

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
import io.novafoundation.nova.feature_staking_impl.domain.staking.bond.BondMoreInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.BondMoreValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.select.SelectBondMorePayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.select.SelectBondMoreViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class SelectBondMoreModule {

    @Provides
    @IntoMap
    @ViewModelKey(SelectBondMoreViewModel::class)
    fun provideViewModel(
        interactor: StakingInteractor,
        router: StakingRouter,
        bondMoreInteractor: BondMoreInteractor,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        validationSystem: BondMoreValidationSystem,
        feeLoaderMixin: FeeLoaderMixin.Presentation,
        payload: SelectBondMorePayload
    ): ViewModel {
        return SelectBondMoreViewModel(
            router,
            interactor,
            bondMoreInteractor,
            resourceManager,
            validationExecutor,
            validationSystem,
            feeLoaderMixin,
            payload
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SelectBondMoreViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectBondMoreViewModel::class.java)
    }
}
