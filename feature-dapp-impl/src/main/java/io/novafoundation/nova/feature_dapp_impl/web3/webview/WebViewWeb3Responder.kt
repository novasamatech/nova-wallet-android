package io.novafoundation.nova.feature_dapp_impl.web3.webview

import io.novafoundation.nova.feature_dapp_impl.web3.Web3Responder

class WebViewWeb3Responder(
    private val webViewHolder: WebViewHolder
) : Web3Responder {

    override fun respondResult(id: String, result: String) {
        evaluateJs(success(id, result))
    }

    override fun respondError(id: String, error: Throwable) {
        evaluateJs(failure(id, error))
    }

    private fun evaluateJs(js: String) = webViewHolder.webView?.post {
        webViewHolder.webView?.evaluateJavascript(js, null)
    }

    private fun success(id: String, result: String) = "window.walletExtension.onAppResponse(\"$id\", $result, null)"

    private fun failure(id: String, error: Throwable) = "window.walletExtension.onAppResponse(\"$id\", null, ${error.message.orEmpty()})"
}
