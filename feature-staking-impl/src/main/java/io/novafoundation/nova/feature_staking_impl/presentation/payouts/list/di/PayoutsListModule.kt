package io.novafoundation.nova.feature_staking_impl.presentation.payouts.list.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.list.PayoutsListViewModel

@Module(includes = [ViewModelModule::class])
class PayoutsListModule {

    @Provides
    @IntoMap
    @ViewModelKey(PayoutsListViewModel::class)
    fun provideViewModel(
        stakingInteractor: StakingInteractor,
        resourceManager: ResourceManager,
        router: StakingRouter,
    ): ViewModel {
        return PayoutsListViewModel(
            router = router,
            resourceManager = resourceManager,
            interactor = stakingInteractor,
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): PayoutsListViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(PayoutsListViewModel::class.java)
    }
}
