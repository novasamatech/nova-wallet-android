package io.novafoundation.nova.feature_dapp_impl.web3

import android.webkit.JavascriptInterface

interface Web3JavascriptListener {

    fun onNewMessage(message: Any?)
}

class WebViewWeb3JavaScriptListener : Web3JavascriptListener {

    private val observers = mutableListOf<Web3JavascriptListener>()

    fun addObserver(observer: Web3JavascriptListener) {
        observers.add(observer)
    }

    fun removeObserver(observer: Web3JavascriptListener) {
        observers.remove(observer)
    }

    @JavascriptInterface
    override fun onNewMessage(message: Any?) {
        observers.onEach { it.onNewMessage(message) }
    }
}
