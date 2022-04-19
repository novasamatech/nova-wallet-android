package io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport

import android.util.Log
import com.google.gson.Gson
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Responder
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewWeb3JavaScriptInterface
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewWeb3Transport
import kotlinx.coroutines.CoroutineScope

class MetamaskTransportFactory(
    private val web3Responder: Web3Responder,
    private val webViewWeb3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    private val gson: Gson,
) {

    fun create(scope: CoroutineScope): MetamaskTransport {
        return MetamaskTransport(
            webViewWeb3JavaScriptInterface = webViewWeb3JavaScriptInterface,
            scope = scope,
            gson = gson,
            web3Responder = web3Responder,
        )
    }
}

class MetamaskTransport(
    private val gson: Gson,
    private val web3Responder: Web3Responder,
    webViewWeb3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    scope: CoroutineScope,
) : WebViewWeb3Transport<MetamaskTransportRequest<*>>(scope, webViewWeb3JavaScriptInterface) {

    override suspend fun messageToRequest(message: String): MetamaskTransportRequest<*>? {
        Log.d(LOG_TAG, message)

        return null
    }
}
