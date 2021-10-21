package io.novafoundation.nova.feature_wallet_impl.data.network.subquery

import io.novafoundation.nova.core.model.Node

class WrongNetworkTypeForSubqueryRequest(message: String) : Exception(message)

fun Node.NetworkType.getSubQueryPath() =
    when (this) {
        Node.NetworkType.POLKADOT -> "fearless-wallet"
        Node.NetworkType.KUSAMA -> "fearless-wallet-ksm"
        Node.NetworkType.WESTEND -> "fearless-wallet-westend"
        else -> throw WrongNetworkTypeForSubqueryRequest("$this is not supported for fetching pending rewards via Subquery")
    }
