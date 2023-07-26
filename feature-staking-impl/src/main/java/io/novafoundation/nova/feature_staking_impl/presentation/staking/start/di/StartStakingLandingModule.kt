package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingUpdateSystem
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.era.StakingEraInteractorFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.ParachainNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingInteractorFactory
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.StartStakingLandingViewModel
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository

@Module(includes = [ViewModelModule::class])
class StartStakingLandingModule {

    @Provides
    fun provideStartStakingInteractorFactory(
        stakingSharedComputation: StakingSharedComputation,
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        stakingEraInteractorFactory: StakingEraInteractorFactory,
        parachainNetworkInfoInteractor: ParachainNetworkInfoInteractor,
        parachainStakingRewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
        nominationPoolGlobalsRepository: NominationPoolGlobalsRepository
    ): StartStakingInteractorFactory {
        return StartStakingInteractorFactory(
            stakingSharedComputation,
            walletRepository,
            accountRepository,
            stakingEraInteractorFactory,
            parachainNetworkInfoInteractor,
            parachainStakingRewardCalculatorFactory,
            nominationPoolGlobalsRepository
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
        startStakingInteractorFactory: StartStakingInteractorFactory,
        appLinksProvider: AppLinksProvider
    ): ViewModel {
        return StartStakingLandingViewModel(
            stakingRouter,
            stakingSharedState,
            resourceManager,
            updateSystem,
            startStakingInteractorFactory,
            appLinksProvider
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
