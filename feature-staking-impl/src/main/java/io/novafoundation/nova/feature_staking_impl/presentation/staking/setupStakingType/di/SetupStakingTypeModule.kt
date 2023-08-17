package io.novafoundation.nova.feature_staking_impl.presentation.staking.setupStakingType.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.setupStakingType.SetupStakingTypeViewModel

@Module(includes = [ViewModelModule::class])
class SetupStakingTypeModule {

    @Provides
    @IntoMap
    @ViewModelKey(SetupStakingTypeViewModel::class)
    fun provideViewModel(
        stakingRouter: StakingRouter
    ): ViewModel {
        return SetupStakingTypeViewModel(stakingRouter)
    }

    @Provides
    fun viewModelCreator(
        fragment: Fragment,
        viewModelProviderFactory: ViewModelProvider.Factory
    ): SetupStakingTypeViewModel {
        return ViewModelProvider(fragment, viewModelProviderFactory).get(SetupStakingTypeViewModel::class.java)
    }
}
