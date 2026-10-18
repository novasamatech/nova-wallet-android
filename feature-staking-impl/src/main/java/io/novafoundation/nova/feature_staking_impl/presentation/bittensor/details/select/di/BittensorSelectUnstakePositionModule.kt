package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.select.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.select.BittensorSelectUnstakePositionCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.select.BittensorSelectUnstakePositionViewModel

@Module(includes = [ViewModelModule::class])
class BittensorSelectUnstakePositionModule {

    @Provides
    @IntoMap
    @ViewModelKey(BittensorSelectUnstakePositionViewModel::class)
    fun provideViewModel(
        request: BittensorSelectUnstakePositionCommunicator.Request,
        communicator: BittensorSelectUnstakePositionCommunicator,
        router: StakingRouter
    ): ViewModel = BittensorSelectUnstakePositionViewModel(request, communicator, router)

    @Provides
    fun provideScreenViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorSelectUnstakePositionViewModel =
        ViewModelProvider(fragment, factory)[BittensorSelectUnstakePositionViewModel::class.java]
}
