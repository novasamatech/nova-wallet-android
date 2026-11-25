package io.novafoundation.nova.feature_assets.presentation.swap.asset.di

import io.novafoundation.nova.feature_swap_api.domain.exchanger.catalogue.ExchangerATaoCatalogue
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_assets.data.repository.PreferencesSwapNetworkSelectionRepository
import io.novafoundation.nova.feature_assets.domain.swap.SwapNetworkSelectionRepository
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.account.SelectedMetaAccountChangeObserver
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchUseCase
import io.novafoundation.nova.feature_assets.domain.swap.SwapTokenSelectionCatalogue
import io.novafoundation.nova.feature_assets.domain.swap.SwapTokenSelectionInteractor
import io.novafoundation.nova.feature_assets.domain.swap.SwapTokenSelectionFilterFactory
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.swap.asset.AssetSwapFlowViewModel
import io.novafoundation.nova.feature_assets.presentation.swap.asset.SwapTokenSelectionFormatter
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapTokenSelectionCommunicator
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class AssetSwapFlowModule {

    @Provides
    fun provideFilterFactory(): SwapTokenSelectionFilterFactory = SwapTokenSelectionFilterFactory()

    @Provides
    fun provideCatalogue(
        chainRegistry: ChainRegistry,
        swapService: SwapService,
        exchangerATaoCatalogue: ExchangerATaoCatalogue,
        filterFactory: SwapTokenSelectionFilterFactory
    ) = SwapTokenSelectionCatalogue(chainRegistry, swapService, exchangerATaoCatalogue, filterFactory)

    @Provides
    fun provideInteractor(
        walletRepository: WalletRepository,
        assetSearchUseCase: AssetSearchUseCase,
        catalogue: SwapTokenSelectionCatalogue,
        filterFactory: SwapTokenSelectionFilterFactory
    ) = SwapTokenSelectionInteractor(walletRepository, assetSearchUseCase, catalogue, filterFactory)

    @Provides
    fun provideNetworkSelectionRepository(preferences: Preferences): SwapNetworkSelectionRepository = PreferencesSwapNetworkSelectionRepository(preferences)

    @Provides
    fun provideSelectedMetaAccountChangeObserver(accountRepository: AccountRepository): SelectedMetaAccountChangeObserver {
        return SelectedMetaAccountChangeObserver(accountRepository)
    }

    @Provides
    fun provideFormatter(resourceManager: ResourceManager, assetIconProvider: AssetIconProvider, amountFormatter: AmountFormatter) =
        SwapTokenSelectionFormatter(resourceManager, assetIconProvider, amountFormatter)

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): AssetSwapFlowViewModel {
        return ViewModelProvider(fragment, factory).get(AssetSwapFlowViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(AssetSwapFlowViewModel::class)
    fun provideViewModel(
        interactor: SwapTokenSelectionInteractor,
        formatter: SwapTokenSelectionFormatter,
        networkSelectionRepository: SwapNetworkSelectionRepository,
        router: AssetsRouter,
        resourceManager: ResourceManager,
        accountRepository: AccountRepository,
        selectedMetaAccountChangeObserver: SelectedMetaAccountChangeObserver,
        communicator: SwapTokenSelectionCommunicator,
        request: SwapTokenSelectionCommunicator.Request
    ): ViewModel = AssetSwapFlowViewModel(
        interactor = interactor,
        formatter = formatter,
        networkSelectionRepository = networkSelectionRepository,
        router = router,
        resourceManager = resourceManager,
        accountRepository = accountRepository,
        selectedMetaAccountChangeObserver = selectedMetaAccountChangeObserver,
        communicator = communicator,
        request = request
    )
}
