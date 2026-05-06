package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.subnetPicker.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorSubnetFetcher
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.subnetPicker.SubtensorSubnetPickerViewModel

@Module(includes = [ViewModelModule::class])
class SubtensorSubnetPickerModule {

    @Provides
    @IntoMap
    @ViewModelKey(SubtensorSubnetPickerViewModel::class)
    fun provideViewModel(
        router: StakingRouter,
        subnetFetcher: SubtensorSubnetFetcher,
    ): ViewModel = SubtensorSubnetPickerViewModel(
        router = router,
        subnetFetcher = subnetFetcher,
    )

    @Provides
    fun provideViewModelInstance(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): SubtensorSubnetPickerViewModel =
        ViewModelProvider(fragment, viewModelFactory).get(SubtensorSubnetPickerViewModel::class.java)
}
