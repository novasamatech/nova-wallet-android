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
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingLandingInfoUpdateSystemFactory
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingUpdaters
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.era.StakingEraInteractorFactory
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.ParachainNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingInteractorFactory
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.StartStakingLandingViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.model.StartStakingLandingPayload
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class StartStakingLandingModule {

    @Provides
    fun provideStartStakingLandingUpdateSystemFactory(
        stakingUpdaters: StakingUpdaters,
        chainRegistry: ChainRegistry,
        storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory
    ): StakingLandingInfoUpdateSystemFactory {
        return StakingLandingInfoUpdateSystemFactory(
            stakingUpdaters,
            chainRegistry,
            storageSharedRequestsBuilderFactory
        )
    }

    @Provides
    fun provideStartStakingInteractorFactory(
        stakingSharedComputation: StakingSharedComputation,
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        stakingEraInteractorFactory: StakingEraInteractorFactory,
        parachainNetworkInfoInteractor: ParachainNetworkInfoInteractor,
        parachainStakingRewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
        nominationPoolGlobalsRepository: NominationPoolGlobalsRepository,
        nominationPoolSharedComputation: NominationPoolSharedComputation,
        chainRegistry: ChainRegistry
    ): StartStakingInteractorFactory {
        return StartStakingInteractorFactory(
            stakingSharedComputation = stakingSharedComputation,
            walletRepository = walletRepository,
            accountRepository = accountRepository,
            stakingEraInteractorFactory = stakingEraInteractorFactory,
            parachainNetworkInfoInteractor = parachainNetworkInfoInteractor,
            parachainStakingRewardCalculatorFactory = parachainStakingRewardCalculatorFactory,
            nominationPoolGlobalsRepository = nominationPoolGlobalsRepository,
            nominationPoolSharedComputation = nominationPoolSharedComputation,
            chainRegistry = chainRegistry
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(StartStakingLandingViewModel::class)
    fun provideViewModel(
        stakingRouter: StakingRouter,
        resourceManager: ResourceManager,
        updateSystemFactory: StakingLandingInfoUpdateSystemFactory,
        startStakingInteractorFactory: StartStakingInteractorFactory,
        appLinksProvider: AppLinksProvider,
        startStakingLandingPayload: StartStakingLandingPayload
    ): ViewModel {
        return StartStakingLandingViewModel(
            stakingRouter,
            resourceManager,
            updateSystemFactory,
            startStakingInteractorFactory,
            appLinksProvider,
            startStakingLandingPayload
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