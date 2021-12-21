package io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.web3.Web3JavascriptResponder
import io.novafoundation.nova.feature_dapp_impl.web3.WebViewHolder
import io.novafoundation.nova.feature_dapp_impl.web3.WebViewWeb3Extension
import io.novafoundation.nova.feature_dapp_impl.web3.WebViewWeb3JavaScriptInterface
import kotlinx.coroutines.CoroutineScope


// should be in tact with javascript_interface_bridge.js
private const val JAVASCRIPT_INTERFACE_NAME = "Nova"

private const val PROVIDER_SCRIPT_NAME = "nova-wallet-provider"
private const val LISTENER_SCRIPT_NAME = "nova-wallet-listener"

class PolkadotJsExtensionFactory(
    private val resourceManager: ResourceManager,
    private val web3JavascriptResponder: Web3JavascriptResponder,
    private val webViewWeb3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    private val webViewHolder: WebViewHolder,
    private val gson: Gson,
) {

    fun create(scope: CoroutineScope): PolkadotJsExtension {
        return PolkadotJsExtension(
            web3JavascriptResponder = web3JavascriptResponder,
            resourceManager = resourceManager,
            webViewWeb3JavaScriptInterface = webViewWeb3JavaScriptInterface,
            scope = scope,
            webViewHolder = webViewHolder,
            gson = gson
        )
    }
}

class PolkadotJsExtension(
    private val gson: Gson,
    private val web3JavascriptResponder: Web3JavascriptResponder,
    private val resourceManager: ResourceManager,
    private val webViewWeb3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    webViewHolder: WebViewHolder,
    scope: CoroutineScope,
) : WebViewWeb3Extension<PolkadotJsExtensionRequest<*>>(scope, webViewWeb3JavaScriptInterface, webViewHolder) {

    override suspend fun messageToRequest(message: String): PolkadotJsExtensionRequest<*>? {
        Log.d(LOG_TAG, message)

        val typeToken = object: TypeToken<Map<String, Any?>>() {}
        val parsedMessage = gson.fromJson<Map<String, Any?>>(message, typeToken.type)

        val url = parsedMessage["url"] as? String

        return when (parsedMessage["msgType"]) {
            PolkadotJsExtensionRequest.Identifier.AUTHORIZE_TAB.id -> url?.let {
                PolkadotJsExtensionRequest.AuthorizeTab(
                    web3JavascriptResponder = web3JavascriptResponder,
                    url = url
                )
            }
            else -> null
        }
    }

    override fun inject(into: WebView) {
        super.inject(into)

        into.addJavascriptInterface(webViewWeb3JavaScriptInterface, JAVASCRIPT_INTERFACE_NAME)

        val providerScript = resourceManager.loadRawString(R.raw.nova_min)
        val listenerScript = resourceManager.loadRawString(R.raw.javascript_interface_bridge)

        into.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                    into.injectJavaScriptOnce(providerScript, PROVIDER_SCRIPT_NAME, appendToStart = true)
                    into.injectJavaScriptOnce(listenerScript, LISTENER_SCRIPT_NAME, appendToStart = false)
            }
        }
    }
}

private fun WebView.injectJavaScriptOnce(
    js: String,
    scriptId: String,
    appendToStart: Boolean
) {
    val encoded: String = Base64.encodeToString(js.encodeToByteArray(), Base64.NO_WRAP)
    val method = if (appendToStart) "prepend" else "appendChild"

    evaluateJavascript(
        """
        var parent = document.getElementsByTagName('body').item(0);
        var prevScripts = parent.getElementsByClassName("$scriptId")
        if (prevScripts.length== 0) {
            var script = document.createElement('script');                 
            script.type = 'text/javascript';
            script.innerHTML = window.atob('$encoded');
            script.className = "$scriptId";
            parent.$method(script);
        }
""".trimIndent(), null
    )
}
