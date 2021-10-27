package io.novafoundation.nova.feature_staking_impl.presentation.validators.current.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validators.current.CurrentValidatorsInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.validators.current.CurrentValidatorsViewModel

@Module(includes = [ViewModelModule::class])
class CurrentValidatorsModule {

    @Provides
    @IntoMap
    @ViewModelKey(CurrentValidatorsViewModel::class)
    fun provideViewModel(
        stakingInteractor: StakingInteractor,
        resourceManager: ResourceManager,
        iconGenerator: AddressIconGenerator,
        currentValidatorsInteractor: CurrentValidatorsInteractor,
        setupStakingSharedState: SetupStakingSharedState,
        router: StakingRouter,
        selectedAssetState: StakingSharedState
    ): ViewModel {
        return CurrentValidatorsViewModel(
            router,
            resourceManager,
            stakingInteractor,
            iconGenerator,
            currentValidatorsInteractor,
            setupStakingSharedState,
            selectedAssetState
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): CurrentValidatorsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(CurrentValidatorsViewModel::class.java)
    }
}
