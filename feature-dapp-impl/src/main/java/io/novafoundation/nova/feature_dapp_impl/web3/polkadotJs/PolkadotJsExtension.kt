package io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs

import android.util.Log
import android.webkit.WebView
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Extension
import io.novafoundation.nova.feature_dapp_impl.web3.Web3JavascriptResponder
import io.novafoundation.nova.feature_dapp_impl.web3.WebViewWeb3Extension
import io.novafoundation.nova.feature_dapp_impl.web3.WebViewWeb3JavaScriptListener
import kotlinx.coroutines.CoroutineScope

class PolkadotJsExtension(
    private val web3JavascriptResponder: Web3JavascriptResponder,
    private val resourceManager: ResourceManager,
    webViewWeb3JavaScriptListener: WebViewWeb3JavaScriptListener,
    scope: CoroutineScope,
) : WebViewWeb3Extension<PolkadotJsExtension.Request<*>>(scope, webViewWeb3JavaScriptListener) {

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
        val mainScript = resourceManager.loadRawString(R.raw.nova_min)
        val javascriptInterfaceBridge = resourceManager.loadRawString(R.raw.javascript_interface_bridge)

        into.evaluateJavascript(mainScript, null)
        into.evaluateJavascript(javascriptInterfaceBridge, null)
    }
}
