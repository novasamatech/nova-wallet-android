package io.novafoundation.nova.feature_dapp_impl.web3

interface Web3JavascriptResponder {

    fun respondResult(id: String, result: String)
    
    fun respondError(id: String, error: Throwable)
}

class WebViewWeb3JavascriptResponder(
    private val webViewHolder: WebViewHolder
): Web3JavascriptResponder {

    override fun respondResult(id: String, result: String) {
        webViewHolder.webView?.evaluateJavascript(success(id, result), null)
    }

    override fun respondError(id: String, error: Throwable) {
        webViewHolder.webView?.evaluateJavascript(failure(id, error), null)
    }

    private fun success(id: String, result: String) = "window.walletExtension.onAppResponse($id, $result, null)"

    private fun failure(id: String, error: Throwable) = "window.walletExtension.onAppResponse($id, null, ${error.message.orEmpty()})"
}
