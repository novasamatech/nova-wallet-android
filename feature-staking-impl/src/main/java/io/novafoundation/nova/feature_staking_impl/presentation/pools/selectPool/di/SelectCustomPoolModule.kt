package io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool.SelectCustomPoolViewModel

@Module(includes = [ViewModelModule::class])
class SelectCustomPoolModule {

    @Provides
    @IntoMap
    @ViewModelKey(SelectCustomPoolViewModel::class)
    fun provideViewModel(stakingRouter: StakingRouter): ViewModel {
        return SelectCustomPoolViewModel(stakingRouter)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SelectCustomPoolViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectCustomPoolViewModel::class.java)
    }
}
