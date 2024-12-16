package io.novafoundation.nova.feature_dapp_core.web3.webView

import android.util.Log
import android.webkit.JavascriptInterface
import io.novafoundation.nova.common.utils.LOG_TAG
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

open class WebViewWeb3JavaScriptInterface {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 3)
    val messages: Flow<String> = _messages

    @JavascriptInterface
    fun onNewMessage(message: String) {
        Log.d(LOG_TAG, message)

        _messages.tryEmit(message)
    }
}

class MetamaskWeb3JavaScriptInterface : WebViewWeb3JavaScriptInterface()

class PolkadotJsWeb3JavaScriptInterface : WebViewWeb3JavaScriptInterface()
