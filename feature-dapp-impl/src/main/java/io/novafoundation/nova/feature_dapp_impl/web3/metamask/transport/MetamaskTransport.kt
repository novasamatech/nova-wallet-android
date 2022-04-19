package io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.common.utils.fromParsedHierarchy
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskChain
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewWeb3JavaScriptInterface
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewWeb3Transport
import kotlinx.coroutines.CoroutineScope

class MetamaskTransportFactory(
    private val responder: MetamaskResponder,
    private val webViewWeb3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    private val gson: Gson,
) {

    fun create(scope: CoroutineScope): MetamaskTransport {
        return MetamaskTransport(
            webViewWeb3JavaScriptInterface = webViewWeb3JavaScriptInterface,
            scope = scope,
            gson = gson,
            responder = responder,
        )
    }
}

private class MetamaskRequest(
    val id: String,
    @SerializedName("name")
    val identifier: String,
    @SerializedName("object")
    val payload: Any?
)

class MetamaskTransport(
    private val gson: Gson,
    private val responder: MetamaskResponder,
    webViewWeb3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    scope: CoroutineScope,
) : WebViewWeb3Transport<MetamaskTransportRequest<*>>(scope, webViewWeb3JavaScriptInterface) {

    override suspend fun messageToRequest(message: String): MetamaskTransportRequest<*>? = runCatching {
        val request = gson.fromJson<MetamaskRequest>(message)

        when(request.identifier) {
            MetamaskTransportRequest.Identifier.REQUEST_ACCOUNTS.id -> {
                MetamaskTransportRequest.RequestAccounts(request.id, gson, responder)
            }
            MetamaskTransportRequest.Identifier.ADD_ETHEREUM_CHAIN.id -> {
                val chain = gson.fromParsedHierarchy<MetamaskChain>(request.payload)

                MetamaskTransportRequest.AddEthereumChain(request.id, gson, responder, chain)
            }
            else -> null
        }

    }
        .onFailure { Log.e(LOG_TAG, "Failed to parse dApp message: $message", it) }
        .getOrNull()
}
