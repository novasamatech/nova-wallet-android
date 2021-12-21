package io.novafoundation.nova.feature_dapp_impl.di.modules

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_dapp_impl.web3.Web3JavascriptResponder
import io.novafoundation.nova.feature_dapp_impl.web3.WebViewHolder
import io.novafoundation.nova.feature_dapp_impl.web3.WebViewWeb3JavaScriptInterface
import io.novafoundation.nova.feature_dapp_impl.web3.WebViewWeb3JavascriptResponder
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsExtensionFactory

@Module
class Web3Module {

    @Provides
    @FeatureScope
    fun provideWebViewHolder() = WebViewHolder()

    @Provides
    @FeatureScope
    fun provideWeb3JavascriptResponder(webViewHolder: WebViewHolder): Web3JavascriptResponder {
        return WebViewWeb3JavascriptResponder(webViewHolder)
    }

    @Provides
    @FeatureScope
    fun provideWeb3JavaScriptInterface() = WebViewWeb3JavaScriptInterface()

    @Provides
    @FeatureScope
    fun providePolkadotJsFactory(
        resourceManager: ResourceManager,
        web3JavascriptResponder: Web3JavascriptResponder,
        web3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
        webViewHolder: WebViewHolder,
        gson: Gson
    ): PolkadotJsExtensionFactory {
        return PolkadotJsExtensionFactory(
            resourceManager = resourceManager,
            web3JavascriptResponder = web3JavascriptResponder,
            webViewWeb3JavaScriptInterface = web3JavaScriptInterface,
            webViewHolder = webViewHolder,
            gson = gson
        )
    }
}
