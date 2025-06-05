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
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.ToastMessageManager
import io.novafoundation.nova.core_db.dao.BrowserHostSettingsDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_dapp_api.data.repository.BrowserHostSettingsRepository
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.data.repository.FavouritesDAppRepository
import io.novafoundation.nova.feature_dapp_impl.data.repository.PhishingSitesRepository
import io.novafoundation.nova.feature_dapp_impl.data.repository.RealBrowserHostSettingsRepository
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.DappBrowserInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DAppBrowserViewModel
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchCommunicator
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.BrowserTabService
import io.novafoundation.nova.feature_dapp_impl.web3.states.ExtensionStoreFactory
import io.novafoundation.nova.common.utils.browser.fileChoosing.WebViewFileChooserFactory
import io.novafoundation.nova.common.utils.browser.permissions.WebViewPermissionAskerFactory
import io.novafoundation.nova.common.utils.webView.WebViewRequestInterceptor
import io.novafoundation.nova.common.utils.webView.interceptors.CompoundWebViewRequestInterceptor
import io.novafoundation.nova.feature_dapp_impl.web3.webview.interceptors.WalletConnectPairingInterceptor
import io.novafoundation.nova.feature_dapp_impl.web3.webview.interceptors.Web3FallbackInterceptor
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectService
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
        fragment: Fragment,
        webViewFileChooserFactory: WebViewFileChooserFactory
    ) = webViewFileChooserFactory.create(fragment)

    @Provides
    @ScreenScope
    fun providePermissionAsker(
        fragment: Fragment,
        webViewPermissionAskerFactory: WebViewPermissionAskerFactory
    ) = webViewPermissionAskerFactory.create(fragment)

    @Provides
    @ScreenScope
    fun provideWebViewClientInterceptor(
        toastMessageManager: ToastMessageManager,
        contextManager: ContextManager,
        walletConnectService: WalletConnectService
    ): WebViewRequestInterceptor {
        return CompoundWebViewRequestInterceptor(
            WalletConnectPairingInterceptor(walletConnectService),
            Web3FallbackInterceptor(toastMessageManager, contextManager)
        )
    }

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
        payload: DAppBrowserPayload,
        extensionStoreFactory: ExtensionStoreFactory,
        dAppInteractor: DappInteractor,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        chainRegistry: ChainRegistry,
        browserTabService: BrowserTabService
    ): ViewModel {
        return DAppBrowserViewModel(
            router = router,
            interactor = interactor,
            dAppInteractor = dAppInteractor,
            selectedAccountUseCase = selectedAccountUseCase,
            signRequester = signRequester,
            dAppSearchRequester = searchRequester,
            payload = payload,
            extensionStoreFactory = extensionStoreFactory,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            chainRegistry = chainRegistry,
            browserTabService = browserTabService
        )
    }
}
