package io.novafoundation.nova.feature_assets.presentation.swap.network.di

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
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.networks.AssetNetworksInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.swap.executor.SwapFlowExecutorFactory
import io.novafoundation.nova.feature_assets.presentation.swap.network.NetworkSwapFlowPayload
import io.novafoundation.nova.feature_assets.presentation.swap.network.NetworkSwapFlowViewModel
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapFlowScopeAggregator
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class NetworkSwapFlowModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): NetworkSwapFlowViewModel {
        return ViewModelProvider(fragment, factory).get(NetworkSwapFlowViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(NetworkSwapFlowViewModel::class)
    fun provideViewModel(
        interactor: AssetNetworksInteractor,
        router: AssetsRouter,
        externalBalancesInteractor: ExternalBalancesInteractor,
        controllableAssetCheck: ControllableAssetCheckMixin,
        accountUseCase: SelectedAccountUseCase,
        resourceManager: ResourceManager,
        executorFactory: SwapFlowExecutorFactory,
        payload: NetworkSwapFlowPayload,
        chainRegistry: ChainRegistry,
        swapFlowScopeAggregator: SwapFlowScopeAggregator
    ): ViewModel {
        return NetworkSwapFlowViewModel(
            interactor = interactor,
            router = router,
            externalBalancesInteractor = externalBalancesInteractor,
            controllableAssetCheck = controllableAssetCheck,
            accountUseCase = accountUseCase,
            resourceManager = resourceManager,
            networkFlowPayload = payload.networkFlowPayload,
            swapFlowPayload = payload.swapFlowPayload,
            chainRegistry = chainRegistry,
            swapFlowExecutor = executorFactory.create(payload.swapFlowPayload),
            swapFlowScopeAggregator = swapFlowScopeAggregator
        )
    }
}
