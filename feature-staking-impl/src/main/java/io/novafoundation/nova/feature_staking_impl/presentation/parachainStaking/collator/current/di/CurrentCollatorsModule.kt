package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.current.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.collator.current.CurrentCollatorInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.collator.current.RealCurrentCollatorInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorConstantsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.current.CurrentCollatorsViewModel
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase

@Module(includes = [ViewModelModule::class])
class CurrentCollatorsModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
        currentRoundRepository: CurrentRoundRepository,
        collatorProvider: CollatorProvider,
    ): CurrentCollatorInteractor = RealCurrentCollatorInteractor(
        parachainStakingConstantsRepository = parachainStakingConstantsRepository,
        currentRoundRepository = currentRoundRepository,
        collatorProvider = collatorProvider
    )

    @Provides
    @IntoMap
    @ViewModelKey(CurrentCollatorsViewModel::class)
    fun provideViewModel(
        router: ParachainStakingRouter,
        resourceManager: ResourceManager,
        iconGenerator: AddressIconGenerator,
        currentCollatorsInteractor: CurrentCollatorInteractor,
        selectedChainStale: StakingSharedState,
        collatorConstantsUseCase: CollatorConstantsUseCase,
        delegatorStateUseCase: DelegatorStateUseCase,
        tokenUseCase: TokenUseCase,
    ): ViewModel {
        return CurrentCollatorsViewModel(
            router = router,
            resourceManager = resourceManager,
            iconGenerator = iconGenerator,
            currentCollatorsInteractor = currentCollatorsInteractor,
            selectedChainStale = selectedChainStale,
            delegatorStateUseCase = delegatorStateUseCase,
            tokenUseCase = tokenUseCase,
            collatorConstantsUseCase = collatorConstantsUseCase
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): CurrentCollatorsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(CurrentCollatorsViewModel::class.java)
    }
}
