package io.novafoundation.nova.feature_dapp_impl.web3

import android.webkit.WebView
import androidx.annotation.CallSuper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

interface Web3Extension<REQUEST : Web3Extension.Request<*>> {

    val requestsFlow: Flow<REQUEST>

    interface Request<RESPONSE> {

        fun accept(response: RESPONSE)

        fun reject(error: Throwable)
    }
}

abstract class WebViewWeb3Extension<R : Web3Extension.Request<*>>(
    scope: CoroutineScope,
    webViewWeb3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    private val webViewHolder: WebViewHolder
) : Web3Extension<R>,
    CoroutineScope by scope {

    override val requestsFlow = webViewWeb3JavaScriptInterface.messages
        .mapNotNull(::messageToRequest)
        .shareIn(this, started = SharingStarted.Eagerly)

    protected abstract suspend fun messageToRequest(message: String): R?

    @CallSuper
    open fun inject(into: WebView) {
        into.prepareForWeb3()

        webViewHolder.set(into)
    }
}
