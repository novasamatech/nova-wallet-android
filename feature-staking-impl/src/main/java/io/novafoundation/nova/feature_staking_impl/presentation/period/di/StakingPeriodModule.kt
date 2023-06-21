package io.novafoundation.nova.feature_staking_impl.presentation.period.di

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
import io.novafoundation.nova.feature_staking_impl.domain.period.StakingRewardPeriodInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.period.StakingPeriodViewModel

@Module(includes = [ViewModelModule::class])
class StakingPeriodModule {

    @Provides
    @IntoMap
    @ViewModelKey(StakingPeriodViewModel::class)
    fun provideViewModel(
        stakingRewardPeriodInteractor: StakingRewardPeriodInteractor,
        singleAssetSharedState: StakingSharedState,
        resourceManager: ResourceManager,
        stakingRouter: StakingRouter
    ): ViewModel {
        return StakingPeriodViewModel(
            stakingRewardPeriodInteractor,
            singleAssetSharedState,
            resourceManager,
            stakingRouter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): StakingPeriodViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(StakingPeriodViewModel::class.java)
    }
}
