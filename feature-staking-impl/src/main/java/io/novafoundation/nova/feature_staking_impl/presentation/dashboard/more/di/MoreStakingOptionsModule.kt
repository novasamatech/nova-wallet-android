package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_staking_api.domain.dashboard.StakingDashboardInteractor
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.StakingDashboardPresentationMapper
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.MoreStakingOptionsViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.components.ComponentsModule

@Module(includes = [ViewModelModule::class, ComponentsModule::class])
class MoreStakingOptionsModule {

    @Provides
    @IntoMap
    @ViewModelKey(MoreStakingOptionsViewModel::class)
    fun provideViewModel(
        interactor: StakingDashboardInteractor,
        router: StakingRouter,
        stakingSharedState: StakingSharedState,
        presentationMapper: StakingDashboardPresentationMapper,
    ): ViewModel {
        return MoreStakingOptionsViewModel(
            interactor = interactor,
            router = router,
            stakingSharedState = stakingSharedState,
            presentationMapper = presentationMapper
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): MoreStakingOptionsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(MoreStakingOptionsViewModel::class.java)
    }
}
