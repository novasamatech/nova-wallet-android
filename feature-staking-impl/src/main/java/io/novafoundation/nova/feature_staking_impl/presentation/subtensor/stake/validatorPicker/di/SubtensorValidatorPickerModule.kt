package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.validatorPicker.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorValidatorProvider
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.validatorPicker.SubtensorValidatorPickerViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SubtensorValidatorPickerModule {

    @Provides
    @IntoMap
    @ViewModelKey(SubtensorValidatorPickerViewModel::class)
    fun provideViewModel(
        router: StakingRouter,
        validatorProvider: SubtensorValidatorProvider,
        stakingSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        netuid: Int,
    ): ViewModel = SubtensorValidatorPickerViewModel(
        router = router,
        validatorProvider = validatorProvider,
        stakingSharedState = stakingSharedState,
        chainRegistry = chainRegistry,
        netuid = netuid,
    )

    @Provides
    fun provideViewModelInstance(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): SubtensorValidatorPickerViewModel =
        ViewModelProvider(fragment, viewModelFactory).get(SubtensorValidatorPickerViewModel::class.java)
}
