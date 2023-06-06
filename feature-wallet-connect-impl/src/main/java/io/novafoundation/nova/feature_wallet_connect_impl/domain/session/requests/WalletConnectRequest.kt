package io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests

import com.walletconnect.web3.wallet.client.Wallet
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignRequest

interface WalletConnectRequest {

    interface Factory {

        fun create(sessionRequest: Wallet.Model.SessionRequest): WalletConnectRequest?
    }

    val id: String

    suspend fun respondWith(response: ExternalSignCommunicator.Response): Result<*>

    fun toExternalSignRequest(): ExternalSignRequest
}
