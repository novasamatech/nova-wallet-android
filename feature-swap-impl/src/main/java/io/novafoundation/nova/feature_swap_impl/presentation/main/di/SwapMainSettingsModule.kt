package io.novafoundation.nova.feature_swap_impl.presentation.main.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.main.SwapMainSettingsViewModel
import io.novafoundation.nova.feature_swap_impl.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SwapMainSettingsModule {

    @Provides
    @IntoMap
    @ViewModelKey(SwapMainSettingsViewModel::class)
    fun provideViewModel(
        swapRouter: SwapRouter,
        swapInteractor: SwapInteractor,
        resourceManager: ResourceManager,
        swapSettingsStateProvider: SwapSettingsStateProvider,
        chainRegistry: ChainRegistry,
        assetUseCase: ArbitraryAssetUseCase
    ): ViewModel {
        return SwapMainSettingsViewModel(
            swapRouter = swapRouter,
            swapInteractor = swapInteractor,
            swapSettingsStateProvider = swapSettingsStateProvider,
            resourceManager = resourceManager,
            chainRegistry = chainRegistry,
            assetUseCase = assetUseCase
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SwapMainSettingsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SwapMainSettingsViewModel::class.java)
    }
}
