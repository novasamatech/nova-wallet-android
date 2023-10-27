package io.novafoundation.nova.feature_assets.presentation.swap.di

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
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.swap.AssetSwapFlowViewModel
import io.novafoundation.nova.feature_assets.presentation.swap.executor.InitialSwapFlowExecutor
import io.novafoundation.nova.feature_assets.presentation.swap.executor.SwapFlowExecutorFactory
import io.novafoundation.nova.feature_assets.presentation.swap.SwapFlowPayload
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class AssetSwapFlowModule {

    @Provides
    fun provideInitialSwapFlowExecutor(
        assetsRouter: AssetsRouter
    ): InitialSwapFlowExecutor {
        return InitialSwapFlowExecutor(assetsRouter)
    }

    @Provides
    fun provideSwapExecutor(
        initialSwapFlowExecutor: InitialSwapFlowExecutor,
        assetsRouter: AssetsRouter,
        swapSettingsStateProvider: SwapSettingsStateProvider,
        chainRegistry: ChainRegistry
    ): SwapFlowExecutorFactory {
        return SwapFlowExecutorFactory(initialSwapFlowExecutor, assetsRouter, swapSettingsStateProvider, chainRegistry)
    }

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): AssetSwapFlowViewModel {
        return ViewModelProvider(fragment, factory).get(AssetSwapFlowViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(AssetSwapFlowViewModel::class)
    fun provideViewModel(
        interactor: AssetSearchInteractor,
        router: AssetsRouter,
        currencyInteractor: CurrencyInteractor,
        externalBalancesInteractor: ExternalBalancesInteractor,
        controllableAssetCheck: ControllableAssetCheckMixin,
        accountUseCase: SelectedAccountUseCase,
        resourceManager: ResourceManager,
        payload: SwapFlowPayload,
        executorFactory: SwapFlowExecutorFactory
    ): ViewModel {
        return AssetSwapFlowViewModel(
            interactor = interactor,
            router = router,
            currencyInteractor = currencyInteractor,
            externalBalancesInteractor = externalBalancesInteractor,
            controllableAssetCheck = controllableAssetCheck,
            accountUseCase = accountUseCase,
            resourceManager = resourceManager,
            swapFlowExecutor = executorFactory.create(payload),
            payload = payload
        )
    }
}
