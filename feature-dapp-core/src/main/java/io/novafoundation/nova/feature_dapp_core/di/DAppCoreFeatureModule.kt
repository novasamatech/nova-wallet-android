package io.novafoundation.nova.feature_dapp_core.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_dapp_core.web3.injector.MetamaskScriptInjector
import io.novafoundation.nova.feature_dapp_core.web3.injector.PolkadotScriptInjector
import io.novafoundation.nova.feature_dapp_core.web3.webView.MetamaskWeb3JavaScriptInterface
import io.novafoundation.nova.feature_dapp_core.web3.webView.PolkadotJsWeb3JavaScriptInterface
import io.novafoundation.nova.feature_dapp_core.web3.webView.WebViewScriptInjector
import io.novafoundation.nova.feature_dapp_core.web3.webView.WebViewWeb3JavaScriptInterface


@Module
class DAppCoreFeatureModule {

    @Provides
    @FeatureScope
    fun provideWebViewScriptInjector(resourceManager: ResourceManager) = WebViewScriptInjector(resourceManager)

    @Provides
    @FeatureScope
    fun provideMetamaskWeb3JavaScriptInterface() = MetamaskWeb3JavaScriptInterface()

    @Provides
    @FeatureScope
    fun providePolkadotWeb3JavaScriptInterface() = PolkadotJsWeb3JavaScriptInterface()

    @Provides
    @FeatureScope
    fun providePolkadotScriptInjector(
        jsInterface: PolkadotJsWeb3JavaScriptInterface,
        webViewScriptInjector: WebViewScriptInjector
    ): PolkadotScriptInjector {
        return PolkadotScriptInjector(
            jsInterface = jsInterface,
            webViewScriptInjector = webViewScriptInjector
        )
    }

    @Provides
    @FeatureScope
    fun provideMetamaskScriptInjector(
        jsInterface: MetamaskWeb3JavaScriptInterface,
        webViewScriptInjector: WebViewScriptInjector
    ): MetamaskScriptInjector {
        return MetamaskScriptInjector(
            jsInterface = jsInterface,
            webViewScriptInjector = webViewScriptInjector
        )
    }
}
