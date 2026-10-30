package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.filters.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.filters.BittensorValidatorFiltersCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.filters.BittensorValidatorFiltersViewModel

@Module(includes = [ViewModelModule::class])
class BittensorValidatorFiltersModule {

    @Provides
    @IntoMap
    @ViewModelKey(BittensorValidatorFiltersViewModel::class)
    fun provideViewModel(
        request: BittensorValidatorFiltersCommunicator.Request,
        communicator: BittensorValidatorFiltersCommunicator,
        router: StakingRouter
    ): ViewModel = BittensorValidatorFiltersViewModel(request, communicator, router)

    @Provides
    fun provideViewModelCreator(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorValidatorFiltersViewModel =
        ViewModelProvider(fragment, factory)[BittensorValidatorFiltersViewModel::class.java]
}
