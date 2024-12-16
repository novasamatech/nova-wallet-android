package io.novafoundation.nova.feature_dapp_core.web3.injector

import android.webkit.WebView
import io.novafoundation.nova.feature_dapp_core.R
import io.novafoundation.nova.feature_dapp_core.web3.webView.WebViewScriptInjector
import io.novafoundation.nova.feature_dapp_core.web3.webView.WebViewWeb3JavaScriptInterface

// should be in tact with javascript_interface_bridge.js
private const val JS_INTERFACE_NAME = "PolkadotJs"

class PolkadotScriptInjector(
    private val jsInterface: WebViewWeb3JavaScriptInterface,
    private val webViewScriptInjector: WebViewScriptInjector
) : Web3ScriptInjector {

    override fun initialInject(into: WebView) {
        webViewScriptInjector.injectJsInterface(into, jsInterface, JS_INTERFACE_NAME)
    }

    override fun injectForPage(into: WebView) {
        webViewScriptInjector.injectScript(R.raw.polkadotjs_min, into, scriptId = "novawallet-polkadotjs-bundle")
        webViewScriptInjector.injectScript(R.raw.javascript_interface_bridge, into, scriptId = "novawallet-polkadotjs-provider")
    }
}
