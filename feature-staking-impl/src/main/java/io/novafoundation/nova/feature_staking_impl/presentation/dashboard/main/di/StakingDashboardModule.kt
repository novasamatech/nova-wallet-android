package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_staking_api.data.dashboard.StakingDashboardUpdateSystem
import io.novafoundation.nova.feature_staking_api.domain.dashboard.StakingDashboardInteractor
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.StakingDashboardPresentationMapper
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.StakingDashboardViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.components.ComponentsModule
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter

@Module(includes = [ViewModelModule::class, ComponentsModule::class])
class StakingDashboardModule {

    @Provides
    @IntoMap
    @ViewModelKey(StakingDashboardViewModel::class)
    fun provideViewModel(
        interactor: StakingDashboardInteractor,
        accountUseCase: SelectedAccountUseCase,
        resourceManager: ResourceManager,
        dashboardUpdateSystem: StakingDashboardUpdateSystem,
        dashboardRouter: StakingDashboardRouter,
        router: StakingRouter,
        stakingSharedState: StakingSharedState,
        presentationMapper: StakingDashboardPresentationMapper,
        startMultiStakingRouter: StartMultiStakingRouter,
        amountFormatter: AmountFormatter
    ): ViewModel {
        return StakingDashboardViewModel(
            interactor = interactor,
            accountUseCase = accountUseCase,
            resourceManager = resourceManager,
            stakingDashboardUpdateSystem = dashboardUpdateSystem,
            dashboardRouter = dashboardRouter,
            router = router,
            stakingSharedState = stakingSharedState,
            presentationMapper = presentationMapper,
            startMultiStakingRouter = startMultiStakingRouter,
            amountFormatter = amountFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): StakingDashboardViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(StakingDashboardViewModel::class.java)
    }
}
