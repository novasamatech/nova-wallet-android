package io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport

import io.novafoundation.nova.feature_dapp_impl.web3.Web3Responder
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewHolder

class MetamaskResponder(private val webViewHolder: WebViewHolder): Web3Responder {

    override fun respondResult(id: String, result: String) {
        TODO("Not yet implemented")
    }

    override fun respondSubscription(id: String, result: String) {
        TODO("Not yet implemented")
    }

    override fun respondError(id: String, error: Throwable) {
        TODO("Not yet implemented")
    }
}
