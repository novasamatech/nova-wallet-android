package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.main.di

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
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.BittensorDelegatesClient
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorPositionCache
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.main.SubtensorStakingViewModel

@Module(includes = [ViewModelModule::class])
class SubtensorStakingModule {

    @Provides
    @IntoMap
    @ViewModelKey(SubtensorStakingViewModel::class)
    fun provideViewModel(
        stakingSharedState: StakingSharedState,
        accountRepository: AccountRepository,
        positionCache: SubtensorPositionCache,
        delegatesClient: BittensorDelegatesClient,
        resourceManager: ResourceManager,
        router: StakingDashboardRouter,
        stakingRouter: StakingRouter,
        addressIconGenerator: AddressIconGenerator,
    ): ViewModel = SubtensorStakingViewModel(
        stakingSharedState = stakingSharedState,
        accountRepository = accountRepository,
        positionCache = positionCache,
        delegatesClient = delegatesClient,
        resourceManager = resourceManager,
        router = router,
        stakingRouter = stakingRouter,
        addressIconGenerator = addressIconGenerator,
    )

    @Provides
    fun provideViewModelInstance(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): SubtensorStakingViewModel = ViewModelProvider(fragment, viewModelFactory).get(SubtensorStakingViewModel::class.java)
}
