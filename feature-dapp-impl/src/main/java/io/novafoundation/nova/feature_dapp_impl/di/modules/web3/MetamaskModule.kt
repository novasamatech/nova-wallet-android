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
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.di.Metamask
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.states.MetamaskStateFactory
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskInjector
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskResponder
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskTransportFactory
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewHolder
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewScriptInjector
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewWeb3JavaScriptInterface
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class MetamaskModule {

    @Provides
    @Metamask
    @FeatureScope
    fun provideWeb3JavaScriptInterface() = WebViewWeb3JavaScriptInterface()

    @Provides
    @FeatureScope
    fun provideInjector(
        gson: Gson,
        webViewScriptInjector: WebViewScriptInjector,
        @Metamask web3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    ) = MetamaskInjector(
        isDebug = BuildConfig.DEBUG,
        gson = gson,
        jsInterface = web3JavaScriptInterface,
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
        @Metamask web3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
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
