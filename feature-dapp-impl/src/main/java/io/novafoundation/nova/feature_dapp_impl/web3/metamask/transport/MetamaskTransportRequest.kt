package io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport

import com.google.gson.Gson
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Transport
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.EthereumAddress
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskChain

sealed class MetamaskTransportRequest<R>(
    val id: String,
    private val gson: Gson,
    protected val responder: MetamaskResponder,
) : Web3Transport.Request<R> {

    override fun reject(error: Throwable) {
        require(error is MetamaskError) {
            "Metamask transport allows only instances of MetamaskError as errors"
        }

       responder.respondError(id, error)
    }

    override fun accept(response: R) {
        if (response is Unit) {
            responder.respondNullResult(id)
        } else {
            responder.respondResult(id, gson.toJson(response))
        }
    }

    enum class Identifier(val id: String) {
        REQUEST_ACCOUNTS("requestAccounts"),
        ADD_ETHEREUM_CHAIN("addEthereumChain"),
        SWITCH_ETHEREUM_CHAIN("switchEthereumChain")
    }

    class RequestAccounts(
        id: String,
        gson: Gson,
        responder: MetamaskResponder
    ) : MetamaskTransportRequest<List<EthereumAddress>>(id, gson, responder)

    class AddEthereumChain(
        id: String,
        gson: Gson,
        responder: MetamaskResponder,
        val chain: MetamaskChain,
    ): MetamaskTransportRequest<Unit>(id, gson, responder)

    class SwitchEthereumChain(
        id: String,
        gson: Gson,
        responder: MetamaskResponder,
        val chainId: String
    ): MetamaskTransportRequest<Unit>(id, gson, responder)
}
