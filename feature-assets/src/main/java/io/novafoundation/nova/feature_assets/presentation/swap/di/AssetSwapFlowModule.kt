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
import io.novafoundation.nova.feature_assets.presentation.send.flow.AssetSendFlowViewModel
import io.novafoundation.nova.feature_assets.presentation.swap.AssetSwapFlowViewModel
import io.novafoundation.nova.feature_assets.presentation.swap.InitialSwapFlowExecutor
import io.novafoundation.nova.feature_assets.presentation.swap.SwapFlowExecutorFactory
import io.novafoundation.nova.feature_assets.presentation.swap.SwapFlowPayload
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor

@Module(includes = [ViewModelModule::class])
class AssetSwapFlowModule {

    @Provides
    private fun provideInitialSwapFlowExecutor(
        assetsRouter: AssetsRouter
    ): InitialSwapFlowExecutor {
        return InitialSwapFlowExecutor(assetsRouter)
    }

    @Provides
    private fun provideSwapExecutor(
        initialSwapFlowExecutor: InitialSwapFlowExecutor
    ): SwapFlowExecutorFactory {
        return SwapFlowExecutorFactory(initialSwapFlowExecutor, initialSwapFlowExecutor)
    }

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): AssetSwapFlowViewModel {
        return ViewModelProvider(fragment, factory).get(AssetSwapFlowViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(AssetSendFlowViewModel::class)
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
            swapFlowExecutor = executorFactory.create(payload)
        )
    }
}
