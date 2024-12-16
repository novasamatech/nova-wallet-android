package io.novafoundation.nova.feature_dapp_core.web3.injector

import android.webkit.WebView
import io.novafoundation.nova.feature_dapp_core.R
import io.novafoundation.nova.feature_dapp_core.web3.webView.WebViewScriptInjector
import io.novafoundation.nova.feature_dapp_core.web3.webView.WebViewWeb3JavaScriptInterface

private const val JS_INTERFACE_NAME = "Metamask"

class MetamaskScriptInjector(
    private val jsInterface: WebViewWeb3JavaScriptInterface,
    private val webViewScriptInjector: WebViewScriptInjector
) : Web3ScriptInjector {

    override fun initialInject(into: WebView) {
        webViewScriptInjector.injectJsInterface(into, jsInterface, JS_INTERFACE_NAME)
    }

    override fun injectForPage(into: WebView) {
        webViewScriptInjector.injectScript(R.raw.metamask_min, into, scriptId = "novawallet-metamask-bundle")
    }
}
