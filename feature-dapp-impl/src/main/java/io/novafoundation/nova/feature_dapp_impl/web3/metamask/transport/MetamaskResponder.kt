package io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewHolder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

sealed class MetamaskError(val errorCode: Int, message: String) : Throwable(message) {

    class Rejected : MetamaskError(4001, "Access rejected")

    class TxSendingFailed : MetamaskError(0, "Failed to sign and send transaction")

    class NoAccounts : MetamaskError(0, "No Ethereum accounts found in selected wallet")

    class SwitchChainNotFound(chainId: ChainId) : MetamaskError(4902, "Chain $chainId not found")
}

class MetamaskResponder(private val webViewHolder: WebViewHolder) {

    fun respondResult(messageId: String, result: String) {
        val js = "window.ethereum.sendResponse($messageId, $result);"

        evaluateJs(js)
    }

    fun respondNullResult(message: String) {
        val js = "window.ethereum.sendNullResponse($message)"

        evaluateJs(js)
    }

    fun respondError(messageId: String, error: MetamaskError) {
        val js = "window.ethereum.sendRpcError($messageId, ${error.errorCode}, \"${error.message}\")"

        evaluateJs(js)
    }

    private fun evaluateJs(js: String) = webViewHolder.webView?.post {
        webViewHolder.webView?.evaluateJavascript(js, null)

        log(js)
    }

    private fun log(message: String) {
        Log.d(LOG_TAG, message)
    }
}
