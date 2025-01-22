package io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport

import android.webkit.WebView
import com.google.gson.Gson
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.web3.states.ExtensionsStore
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3Injector
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewScriptInjector
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewWeb3JavaScriptInterface

private const val JS_INTERFACE_NAME = "Metamask"

private class ProviderConfig(
    val chainId: String,
    val rpcUrl: String?,
    val isDebug: Boolean,
    val address: String?
)

class MetamaskInjector(
    private val isDebug: Boolean,
    private val gson: Gson,
    private val jsInterface: WebViewWeb3JavaScriptInterface,
    private val webViewScriptInjector: WebViewScriptInjector
) : Web3Injector {

    override fun initialInject(into: WebView) {
        webViewScriptInjector.injectJsInterface(into, jsInterface, JS_INTERFACE_NAME)
    }

    override fun injectForPage(into: WebView, extensionStore: ExtensionsStore) {
        webViewScriptInjector.injectScript(R.raw.metamask_min, into, scriptId = "novawallet-metamask-bundle")
        injectProvider(extensionStore, into)
    }

    private fun injectProvider(extensionStore: ExtensionsStore, into: WebView) {
        val state = extensionStore.metamask.state.value
        val chain = state.chain

        val rpcUrl = chain.rpcUrls.firstOrNull()
        val providerConfig = ProviderConfig(chain.chainId, rpcUrl, isDebug, state.selectedAccountAddress)
        val providerConfigJson = gson.toJson(providerConfig)

        val content = """
                window.ethereum = new novawallet.Provider($providerConfigJson);
                window.web3 = new novawallet.Web3(window.ethereum);
                novawallet.postMessage = (jsonString) => {
                        Nova_Metamask.onNewMessage(JSON.stringify(jsonString))
                };
        """.trimIndent()

        webViewScriptInjector.injectScript(content, into, scriptId = "novawallet-metamask-provider")
    }
}
