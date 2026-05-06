package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.setup.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorPositionCache
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorSubnetFetcher
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.SubtensorStakeSubmitInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.setup.SubtensorStakeSetupViewModel

@Module(includes = [ViewModelModule::class])
class SubtensorStakeSetupModule {

    @Provides
    fun provideSubtensorStakeSubmitInteractor(
        extrinsicService: ExtrinsicService,
        stakingSharedState: StakingSharedState,
        accountRepository: AccountRepository,
        subnetFetcher: SubtensorSubnetFetcher,
        positionCache: SubtensorPositionCache,
    ): SubtensorStakeSubmitInteractor = SubtensorStakeSubmitInteractor(
        extrinsicService = extrinsicService,
        stakingSharedState = stakingSharedState,
        accountRepository = accountRepository,
        subnetFetcher = subnetFetcher,
        positionCache = positionCache,
    )

    @Provides
    @IntoMap
    @ViewModelKey(SubtensorStakeSetupViewModel::class)
    fun provideViewModel(
        router: StakingRouter,
        dashboardRouter: StakingDashboardRouter,
        resourceManager: ResourceManager,
        stakingSharedState: StakingSharedState,
        submitInteractor: SubtensorStakeSubmitInteractor,
        netuid: Int,
        subnetName: String?,
    ): ViewModel = SubtensorStakeSetupViewModel(
        router = router,
        dashboardRouter = dashboardRouter,
        resourceManager = resourceManager,
        stakingSharedState = stakingSharedState,
        submitInteractor = submitInteractor,
        netuid = netuid,
        subnetName = subnetName,
    )

    @Provides
    fun provideViewModelInstance(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): SubtensorStakeSetupViewModel = ViewModelProvider(fragment, viewModelFactory).get(SubtensorStakeSetupViewModel::class.java)
}
