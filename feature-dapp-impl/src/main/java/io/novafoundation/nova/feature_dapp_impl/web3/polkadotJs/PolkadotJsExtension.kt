package io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Responder
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewWeb3Extension
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewWeb3JavaScriptInterface
import kotlinx.coroutines.CoroutineScope

class PolkadotJsExtensionFactory(
    private val web3Responder: Web3Responder,
    private val webViewWeb3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    private val gson: Gson,
) {

    fun create(scope: CoroutineScope): PolkadotJsExtension {
        return PolkadotJsExtension(
            webViewWeb3JavaScriptInterface = webViewWeb3JavaScriptInterface,
            scope = scope,
            gson = gson,
            web3Responder = web3Responder
        )
    }
}

class PolkadotJsExtension(
    private val gson: Gson,
    private val web3Responder: Web3Responder,
    webViewWeb3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    scope: CoroutineScope,
) : WebViewWeb3Extension<PolkadotJsExtensionRequest<*>>(scope, webViewWeb3JavaScriptInterface) {

    override suspend fun messageToRequest(message: String): PolkadotJsExtensionRequest<*>? {
        Log.d(LOG_TAG, message)

        val typeToken = object : TypeToken<Map<String, Any?>>() {}
        val parsedMessage = gson.fromJson<Map<String, Any?>>(message, typeToken.type)

        val url = parsedMessage["url"] as? String

        return when (parsedMessage["msgType"]) {
            PolkadotJsExtensionRequest.Identifier.AUTHORIZE_TAB.id -> url?.let {
                PolkadotJsExtensionRequest.AuthorizeTab(
                    web3Responder = web3Responder,
                    url = url
                )
            }
            else -> null
        }
    }
}
