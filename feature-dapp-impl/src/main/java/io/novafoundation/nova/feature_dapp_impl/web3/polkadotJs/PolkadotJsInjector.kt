package io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs

import android.webkit.WebView
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.web3.states.ExtensionsStore
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3Injector
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewScriptInjector
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewWeb3JavaScriptInterface

// should be in tact with javascript_interface_bridge.js
private const val JS_INTERFACE_NAME = "PolkadotJs"

class PolkadotJsInjector(
    private val jsInterface: WebViewWeb3JavaScriptInterface,
    private val webViewScriptInjector: WebViewScriptInjector
) : Web3Injector {

    override fun initialInject(into: WebView) {
        webViewScriptInjector.injectJsInterface(into, jsInterface, JS_INTERFACE_NAME)
    }

    override fun injectForPage(into: WebView, extensionStore: ExtensionsStore) {
        webViewScriptInjector.injectScript(R.raw.polkadotjs_min, into, scriptId = "novawallet-polkadotjs-bundle")
        webViewScriptInjector.injectScript(R.raw.javascript_interface_bridge, into, scriptId = "novawallet-polkadotjs-provider")
    }
}
