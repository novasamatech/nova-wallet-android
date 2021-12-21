package io.novafoundation.nova.feature_dapp_impl.web3

import kotlinx.coroutines.flow.Flow

interface Web3Extension<REQUEST : Web3Extension.Request<*>> {

    val requestsFlow: Flow<REQUEST>

    interface Request<RESPONSE> {

        fun accept(response: RESPONSE)

        fun reject(error: Throwable)
    }
}
