package io.novafoundation.nova.feature_dapp_impl.di.modules.web3

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_dapp_impl.BuildConfig
import io.novafoundation.nova.feature_dapp_impl.domain.browser.metamask.MetamaskInteractor
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.di.Metamask
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.states.MetamaskStateFactory
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskInjector
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskResponder
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskTransportFactory
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.di.PolkadotJs
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewHolder
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewScriptInjector
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewWeb3JavaScriptInterface

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
        web3Responder: MetamaskResponder,
        @PolkadotJs web3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
        gson: Gson
    ): MetamaskTransportFactory {
        return MetamaskTransportFactory(
            webViewWeb3JavaScriptInterface = web3JavaScriptInterface,
            gson = gson,
            web3Responder = web3Responder,
        )
    }

    @Provides
    @FeatureScope
    fun provideInteractor() = MetamaskInteractor()

    @Provides
    @FeatureScope
    fun provideStateFactory(): MetamaskStateFactory {
        return MetamaskStateFactory()
    }
}
