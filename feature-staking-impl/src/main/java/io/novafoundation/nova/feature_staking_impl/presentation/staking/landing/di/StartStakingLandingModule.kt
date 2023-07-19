package io.novafoundation.nova.feature_staking_impl.presentation.staking.landing.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.landing.StartStakingLandingViewModel

@Module(includes = [ViewModelModule::class])
class StartStakingLandingModule {

    @Provides
    @IntoMap
    @ViewModelKey(StartStakingLandingViewModel::class)
    fun provideViewModel(
        stakingRouter: StakingRouter,
        stakingSharedState: StakingSharedState,
        resourceManager: ResourceManager
    ): ViewModel {
        return StartStakingLandingViewModel(
            stakingRouter,
            stakingSharedState,
            resourceManager
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): StartStakingLandingViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(StartStakingLandingViewModel::class.java)
    }
}
