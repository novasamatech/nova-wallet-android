package io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport

import android.webkit.WebView
import com.google.gson.Gson
import io.novafoundation.nova.feature_dapp_impl.web3.states.ExtensionsStore
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3ProviderInjector
import io.novafoundation.nova.feature_dapp_core.web3.webView.WebViewScriptInjector

private class ProviderConfig(
    val chainId: String,
    val rpcUrl: String?,
    val isDebug: Boolean,
    val address: String?
)

class MetamaskProviderInjector(
    private val isDebug: Boolean,
    private val gson: Gson,
    private val webViewScriptInjector: WebViewScriptInjector
) : Web3ProviderInjector {


    override fun injectProvider(into: WebView, extensionStore: ExtensionsStore) {
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
