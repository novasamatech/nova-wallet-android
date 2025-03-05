package io.novafoundation.nova.feature_staking_impl.presentation.mythos.currentCollators.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.currentCollators.MythosCurrentCollatorsInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.currentCollators.MythosCurrentCollatorsViewModel
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase

@Module(includes = [ViewModelModule::class])
class MythosCurrentCollatorsModule {

    @Provides
    @IntoMap
    @ViewModelKey(MythosCurrentCollatorsViewModel::class)
    fun provideViewModel(
        router: MythosStakingRouter,
        resourceManager: ResourceManager,
        iconGenerator: AddressIconGenerator,
        currentCollatorsInteractor: MythosCurrentCollatorsInteractor,
        stakingSharedState: StakingSharedState,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        tokenUseCase: TokenUseCase,
    ): ViewModel {
        return MythosCurrentCollatorsViewModel(
            router = router,
            resourceManager = resourceManager,
            iconGenerator = iconGenerator,
            currentCollatorsInteractor = currentCollatorsInteractor,
            stakingSharedState = stakingSharedState,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            tokenUseCase = tokenUseCase
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): MythosCurrentCollatorsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(MythosCurrentCollatorsViewModel::class.java)
    }
}
