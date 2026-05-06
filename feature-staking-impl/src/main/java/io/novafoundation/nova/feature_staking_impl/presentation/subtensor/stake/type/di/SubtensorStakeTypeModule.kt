package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.type.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.type.SubtensorStakeTypeViewModel

@Module(includes = [ViewModelModule::class])
class SubtensorStakeTypeModule {

    @Provides
    @IntoMap
    @ViewModelKey(SubtensorStakeTypeViewModel::class)
    fun provideViewModel(
        router: StakingRouter,
    ): ViewModel = SubtensorStakeTypeViewModel(
        router = router,
    )

    @Provides
    fun provideViewModelInstance(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): SubtensorStakeTypeViewModel = ViewModelProvider(fragment, viewModelFactory).get(SubtensorStakeTypeViewModel::class.java)
}
