package io.novafoundation.nova.feature_swap_impl.presentation.route.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapStateStoreProvider
import io.novafoundation.nova.feature_swap_impl.presentation.route.SwapRouteViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SwapRouteModule {

    @Provides
    @IntoMap
    @ViewModelKey(SwapRouteViewModel::class)
    fun provideViewModel(
        swapInteractor: SwapInteractor,
        swapStateStoreProvider: SwapStateStoreProvider,
        chainRegistry: ChainRegistry,
        assetIconProvider: AssetIconProvider,
        resourceManager: ResourceManager,
        router: SwapRouter,
    ): ViewModel {
        return SwapRouteViewModel(
            swapInteractor = swapInteractor,
            swapStateStoreProvider = swapStateStoreProvider,
            chainRegistry = chainRegistry,
            assetIconProvider = assetIconProvider,
            router = router,
            resourceManager = resourceManager
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SwapRouteViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SwapRouteViewModel::class.java)
    }
}
