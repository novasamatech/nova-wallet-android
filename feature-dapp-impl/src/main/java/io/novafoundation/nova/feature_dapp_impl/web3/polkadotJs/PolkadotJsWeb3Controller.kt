package io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs

import android.webkit.WebView
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3Controller
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewScriptInjector

class PolkadotJsWeb3Controller(
    private val webViewScriptInjector: WebViewScriptInjector
) : Web3Controller {

    override fun initialInject(into: WebView) {
        webViewScriptInjector.injectJsInterface(into)
    }

    override fun injectForPage(into: WebView, url: String) {
        webViewScriptInjector.injectScript(R.raw.nova_min, into, WebViewScriptInjector.InjectionPosition.END)
        webViewScriptInjector.injectScript(R.raw.javascript_interface_bridge, into, WebViewScriptInjector.InjectionPosition.END)
    }
}
