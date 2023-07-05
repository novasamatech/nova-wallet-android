package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingUpdateSystem
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.era.StakingEraInteractorFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.direct.DirectStartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingInteractorFactory
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.StartStakingLandingViewModel
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module(includes = [ViewModelModule::class])
class StartStakingLandingModule {

    @Provides
    fun provideStartStakingInteractorFactory(
        stakingSharedState: StakingSharedState,
        stakingSharedComputation: StakingSharedComputation,
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        stakingEraInteractorFactory: StakingEraInteractorFactory
    ): StartStakingInteractorFactory {
        return StartStakingInteractorFactory(
            stakingSharedState,
            stakingSharedComputation,
            walletRepository,
            accountRepository,
            stakingEraInteractorFactory
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(StartStakingLandingViewModel::class)
    fun provideViewModel(
        stakingRouter: StakingRouter,
        stakingSharedState: StakingSharedState,
        resourceManager: ResourceManager,
        updateSystem: StakingUpdateSystem,
        startStakingInteractorFactory: StartStakingInteractorFactory
    ): ViewModel {
        return StartStakingLandingViewModel(
            stakingRouter,
            stakingSharedState,
            resourceManager,
            updateSystem,
            startStakingInteractorFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): StartStakingLandingViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(StartStakingLandingViewModel::class.java)
    }
}
