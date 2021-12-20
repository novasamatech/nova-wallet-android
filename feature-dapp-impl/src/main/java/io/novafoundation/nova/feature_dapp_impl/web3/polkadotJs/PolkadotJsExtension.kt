package io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs

import android.util.Log
import android.webkit.WebView
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Extension
import io.novafoundation.nova.feature_dapp_impl.web3.Web3JavascriptResponder
import io.novafoundation.nova.feature_dapp_impl.web3.WebViewHolder
import io.novafoundation.nova.feature_dapp_impl.web3.WebViewWeb3Extension
import io.novafoundation.nova.feature_dapp_impl.web3.WebViewWeb3JavaScriptInterface
import kotlinx.coroutines.CoroutineScope

// should be in tact with javascript_interface_bridge.js
private const val JAVASCRIPT_INTERFACE_NAME = "Nova"

class PolkadotJsExtensionFactory(
    private val resourceManager: ResourceManager,
    private val web3JavascriptResponder: Web3JavascriptResponder,
    private val webViewWeb3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    private val webViewHolder: WebViewHolder,
) {

    fun create(scope: CoroutineScope): PolkadotJsExtension {
        return PolkadotJsExtension(
            web3JavascriptResponder = web3JavascriptResponder,
            resourceManager = resourceManager,
            webViewWeb3JavaScriptInterface = webViewWeb3JavaScriptInterface,
            scope = scope,
            webViewHolder = webViewHolder
        )
    }
}

class PolkadotJsExtension(
    private val web3JavascriptResponder: Web3JavascriptResponder,
    private val resourceManager: ResourceManager,
    private val webViewWeb3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    webViewHolder: WebViewHolder,
    scope: CoroutineScope,
) : WebViewWeb3Extension<PolkadotJsExtension.Request<*>>(scope, webViewWeb3JavaScriptInterface, webViewHolder) {

    sealed class Request<R>(
        private val web3JavascriptResponder: Web3JavascriptResponder,
        private val identifier: Identifier
    ) : Web3Extension.Request<R> {

        enum class Identifier(val id: String) {
            AUTHORIZE_TAB("pub(authorize.tab)")
        }

        abstract fun serializeResponse(response: R): String

        override fun reject(error: Throwable) {
            web3JavascriptResponder.respondError(identifier.id, error)
        }

        override fun accept(response: R) {
            web3JavascriptResponder.respondResult(identifier.id, serializeResponse(response))
        }

        class AuthorizeTab(
            web3JavascriptResponder: Web3JavascriptResponder,
            val url: String
        ) : Request<AuthorizeTab.Response>(web3JavascriptResponder, Identifier.AUTHORIZE_TAB) {

            class Response(val authorized: Boolean)

            override fun serializeResponse(response: Response): String {
                return response.authorized.toString()
            }
        }
    }

    override fun onNewMessage(message: Any?) {
        Log.d(LOG_TAG, message.toString())

        val messageParams = (message as? Map<*, *>) ?: return

        val url = messageParams["url"] as? String

        val request = when (messageParams["type"]) {
            Request.Identifier.AUTHORIZE_TAB.id -> url?.let {
                Request.AuthorizeTab(
                    web3JavascriptResponder = web3JavascriptResponder,
                    url = url
                )
            }
            else -> null
        }
        
        request?.let { 
            emitRequest(request)
        }
    }

    override fun inject(into: WebView) {
        super.inject(into)

        val mainScript = resourceManager.loadRawString(R.raw.nova_min)
        val javascriptInterfaceBridge = resourceManager.loadRawString(R.raw.javascript_interface_bridge)

        into.evaluateJavascript(mainScript, null)
        into.evaluateJavascript(javascriptInterfaceBridge, null)
        into.addJavascriptInterface(webViewWeb3JavaScriptInterface, JAVASCRIPT_INTERFACE_NAME)
    }
}
