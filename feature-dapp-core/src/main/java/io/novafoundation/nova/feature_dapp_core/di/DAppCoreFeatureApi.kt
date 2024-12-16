package io.novafoundation.nova.feature_dapp_core.di

import io.novafoundation.nova.feature_dapp_core.web3.injector.MetamaskScriptInjector
import io.novafoundation.nova.feature_dapp_core.web3.injector.PolkadotScriptInjector
import io.novafoundation.nova.feature_dapp_core.web3.webView.MetamaskWeb3JavaScriptInterface
import io.novafoundation.nova.feature_dapp_core.web3.webView.PolkadotJsWeb3JavaScriptInterface
import io.novafoundation.nova.feature_dapp_core.web3.webView.WebViewScriptInjector
import io.novafoundation.nova.feature_dapp_core.web3.webView.WebViewWeb3JavaScriptInterface

interface DAppCoreFeatureApi {

    val webViewScriptInjector: WebViewScriptInjector

    val metamaskScriptInjector: MetamaskScriptInjector

    val polkadotJsScriptInjector: PolkadotScriptInjector

    fun polkadotWeb3JavaScriptInterface(): PolkadotJsWeb3JavaScriptInterface

    fun metamaskWeb3JavaScriptInterface(): MetamaskWeb3JavaScriptInterface
}
