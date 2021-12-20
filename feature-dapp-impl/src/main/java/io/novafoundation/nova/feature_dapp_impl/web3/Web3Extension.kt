package io.novafoundation.nova.feature_dapp_impl.web3

import android.webkit.WebView
import androidx.annotation.CallSuper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
    CoroutineScope by scope,
    Web3JavascriptListener {

    override val requestsFlow = MutableSharedFlow<R>()

    init {
        launch {
            webViewWeb3JavaScriptInterface.addObserver(this@WebViewWeb3Extension)
        }
            .invokeOnCompletion { webViewWeb3JavaScriptInterface.removeObserver(this) }
    }

    protected fun emitRequest(request: R) = launch {
        requestsFlow.emit(request)
    }

    abstract override fun onNewMessage(message: Any?)

    @CallSuper
    open fun inject(into: WebView) {
        webViewHolder.set(into)
    }
}
