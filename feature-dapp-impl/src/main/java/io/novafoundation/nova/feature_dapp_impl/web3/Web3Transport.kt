package io.novafoundation.nova.feature_dapp_impl.web3

import kotlinx.coroutines.flow.Flow

interface Web3Transport<REQUEST : Web3Transport.Request<*>> {

    val requestsFlow: Flow<REQUEST>

    interface Request<RESPONSE> {

        fun updateChain(chainId: String, rpcUrl: String)

        fun accept(response: RESPONSE)

        fun reject(error: Throwable)
    }
}

fun Web3Transport.Request<Unit>.accept() = accept(Unit)
