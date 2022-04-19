package io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport

import io.novafoundation.nova.feature_dapp_impl.web3.Web3Responder
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Transport

sealed class MetamaskTransportRequest<R>(
    protected val web3Responder: Web3Responder,
    protected val identifier: Identifier,
) : Web3Transport.Request<R> {

    enum class Identifier(val id: String) {
    }
}
