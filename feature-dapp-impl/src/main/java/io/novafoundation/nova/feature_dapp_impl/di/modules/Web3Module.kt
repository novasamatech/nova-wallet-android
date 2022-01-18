package io.novafoundation.nova.feature_dapp_impl.di.modules

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.core_db.dao.DappAuthorizationDao
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Responder
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsExtensionFactory
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsWeb3Controller
import io.novafoundation.nova.feature_dapp_impl.web3.session.DbWeb3Session
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3WebViewClientFactory
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewHolder
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewScriptInjector
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewWeb3JavaScriptInterface
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewWeb3Responder

@Module
class Web3Module {

    @Provides
    @FeatureScope
    fun provideWebViewHolder() = WebViewHolder()

    @Provides
    @FeatureScope
    fun provideWeb3JavascriptResponder(webViewHolder: WebViewHolder): Web3Responder {
        return WebViewWeb3Responder(webViewHolder)
    }

    @Provides
    @FeatureScope
    fun provideWeb3JavaScriptInterface() = WebViewWeb3JavaScriptInterface()

    @Provides
    @FeatureScope
    fun provideScriptInjector(
        resourceManager: ResourceManager,
        web3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    ) = WebViewScriptInjector(web3JavaScriptInterface, resourceManager)

    @Provides
    @FeatureScope
    fun providePolkadotJsWeb3Controller(
        webViewScriptInjector: WebViewScriptInjector
    ) = PolkadotJsWeb3Controller(webViewScriptInjector)

    @Provides
    @FeatureScope
    fun provideWeb3ClientFactory(
        polkadotJsWeb3Controller: PolkadotJsWeb3Controller,
    ) = Web3WebViewClientFactory(
        controllers = listOf(
            polkadotJsWeb3Controller
        )
    )

    @Provides
    @FeatureScope
    fun provideWeb3Session(
        dappAuthorizationDao: DappAuthorizationDao
    ): Web3Session = DbWeb3Session(dappAuthorizationDao)

    @Provides
    @FeatureScope
    fun providePolkadotJsFactory(
        web3Responder: Web3Responder,
        web3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
        web3Session: Web3Session,
        gson: Gson
    ): PolkadotJsExtensionFactory {
        return PolkadotJsExtensionFactory(
            webViewWeb3JavaScriptInterface = web3JavaScriptInterface,
            gson = gson,
            web3Responder = web3Responder,
            web3Session = web3Session
        )
    }
}
