package io.novafoundation.nova.feature_dapp_impl.web3.webview

import io.novafoundation.nova.feature_dapp_impl.web3.Web3Transport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn

abstract class WebViewWeb3Transport<R : Web3Transport.Request<*>>(
    scope: CoroutineScope,
    webViewWeb3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
) : Web3Transport<R>,
    CoroutineScope by scope {

    override val requestsFlow = webViewWeb3JavaScriptInterface.messages
        .mapNotNull(::messageToRequest)
        .shareIn(this, started = SharingStarted.Eagerly)

    protected abstract suspend fun messageToRequest(message: String): R?
}
