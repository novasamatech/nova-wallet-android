package io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport

import com.google.gson.Gson
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Transport
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.EthereumAddress

sealed class MetamaskTransportRequest<R>(
    val id: String,
    private val gson: Gson,
    protected val responder: MetamaskResponder,
    protected val identifier: Identifier,
) : Web3Transport.Request<R> {

    override fun reject(error: Throwable) {
        require(error is MetamaskError) {
            "Metamask transport allows only instances of MetamaskError as errors"
        }

       responder.respondError(id, error)
    }

    override fun accept(response: R) {
        responder.respondResult(id, gson.toJson(response))
    }

    enum class Identifier(val id: String) {
        REQUEST_ACCOUNTS("requestAccounts")
    }

    class RequestAccounts(
        id: String,
        gson: Gson,
        responder: MetamaskResponder
    ) : MetamaskTransportRequest<List<EthereumAddress>>(id, gson, responder, Identifier.REQUEST_ACCOUNTS)
}
