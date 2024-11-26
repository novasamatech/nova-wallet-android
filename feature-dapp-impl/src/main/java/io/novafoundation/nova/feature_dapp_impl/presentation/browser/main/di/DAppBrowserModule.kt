package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.core_db.dao.BrowserHostSettingsDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_dapp_api.data.repository.BrowserHostSettingsRepository
import io.novafoundation.nova.feature_dapp_api.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.data.repository.FavouritesDAppRepository
import io.novafoundation.nova.feature_dapp_impl.data.repository.PhishingSitesRepository
import io.novafoundation.nova.feature_dapp_impl.data.repository.RealBrowserHostSettingsRepository
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.DappBrowserInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DAppBrowserViewModel
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchCommunicator
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.BrowserTabPoolService
import io.novafoundation.nova.feature_dapp_impl.web3.states.ExtensionStoreFactory
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewFileChooser
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class DAppBrowserModule {

    @Provides
    @ScreenScope
    fun provideBrowserHostSettingsRepository(
        browserHostSettingsDao: BrowserHostSettingsDao
    ): BrowserHostSettingsRepository = RealBrowserHostSettingsRepository(browserHostSettingsDao)

    @Provides
    @ScreenScope
    fun provideInteractor(
        phishingSitesRepository: PhishingSitesRepository,
        favouritesDAppRepository: FavouritesDAppRepository,
        browserHostSettingsRepository: BrowserHostSettingsRepository
    ) = DappBrowserInteractor(
        phishingSitesRepository = phishingSitesRepository,
        favouritesDAppRepository = favouritesDAppRepository,
        browserHostSettingsRepository = browserHostSettingsRepository
    )

    @Provides
    @ScreenScope
    fun provideFileChooser(
        fragment: Fragment
    ) = WebViewFileChooser(fragment)

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): DAppBrowserViewModel {
        return ViewModelProvider(fragment, factory).get(DAppBrowserViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(DAppBrowserViewModel::class)
    fun provideViewModel(
        router: DAppRouter,
        interactor: DappBrowserInteractor,
        selectedAccountUseCase: SelectedAccountUseCase,
        signRequester: ExternalSignCommunicator,
        searchRequester: DAppSearchCommunicator,
        initialUrl: String,
        extensionStoreFactory: ExtensionStoreFactory,
        dAppInteractor: DappInteractor,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        chainRegistry: ChainRegistry,
        browserTabPoolService: BrowserTabPoolService
    ): ViewModel {
        return DAppBrowserViewModel(
            router = router,
            interactor = interactor,
            dAppInteractor = dAppInteractor,
            selectedAccountUseCase = selectedAccountUseCase,
            signRequester = signRequester,
            dAppSearchRequester = searchRequester,
            initialUrl = initialUrl,
            extensionStoreFactory = extensionStoreFactory,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            chainRegistry = chainRegistry,
            browserTabPoolService = browserTabPoolService
        )
    }
}
