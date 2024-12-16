package io.novafoundation.nova.feature_dapp_impl.di.modules.web3

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_dapp_impl.BuildConfig
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.metamask.MetamaskInteractor
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.states.MetamaskStateFactory
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskProviderInjector
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskResponder
import io.novafoundation.nova.feature_dapp_core.web3.injector.MetamaskScriptInjector
import io.novafoundation.nova.feature_dapp_core.web3.webView.MetamaskWeb3JavaScriptInterface
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskTransportFactory
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewHolder
import io.novafoundation.nova.feature_dapp_core.web3.webView.WebViewScriptInjector
import io.novafoundation.nova.feature_dapp_core.web3.webView.WebViewWeb3JavaScriptInterface
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class MetamaskModule {

    @Provides
    @FeatureScope
    fun provideProviderInjector(
        gson: Gson,
        webViewScriptInjector: WebViewScriptInjector,
    ) = MetamaskProviderInjector(
        isDebug = BuildConfig.DEBUG,
        gson = gson,
        webViewScriptInjector = webViewScriptInjector
    )

    @Provides
    @FeatureScope
    fun provideResponder(webViewHolder: WebViewHolder): MetamaskResponder {
        return MetamaskResponder(webViewHolder)
    }

    @Provides
    @FeatureScope
    fun provideTransportFactory(
        responder: MetamaskResponder,
        web3JavaScriptInterface: MetamaskWeb3JavaScriptInterface,
        gson: Gson
    ): MetamaskTransportFactory {
        return MetamaskTransportFactory(
            webViewWeb3JavaScriptInterface = web3JavaScriptInterface,
            gson = gson,
            responder = responder,
        )
    }

    @Provides
    @FeatureScope
    fun provideInteractor(
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry
    ) = MetamaskInteractor(accountRepository, chainRegistry)

    @Provides
    @FeatureScope
    fun provideStateFactory(
        interactor: MetamaskInteractor,
        commonInteractor: DappInteractor,
        resourceManager: ResourceManager,
        addressIconGenerator: AddressIconGenerator,
        web3Session: Web3Session,
        walletUiUseCase: WalletUiUseCase,
    ): MetamaskStateFactory {
        return MetamaskStateFactory(
            interactor = interactor,
            commonInteractor = commonInteractor,
            resourceManager = resourceManager,
            addressIconGenerator = addressIconGenerator,
            web3Session = web3Session,
            walletUiUseCase = walletUiUseCase
        )
    }
}
