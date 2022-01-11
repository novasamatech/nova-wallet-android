package io.novafoundation.nova.feature_dapp_impl.web3

interface Web3Responder {

    fun respondResult(id: String, result: String)

    fun respondSubscription(id: String, result: String)

    fun respondError(id: String, error: Throwable)
}
