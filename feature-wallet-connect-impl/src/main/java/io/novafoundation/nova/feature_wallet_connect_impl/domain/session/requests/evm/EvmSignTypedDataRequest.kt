package io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests.evm

import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Wallet.Params.SessionRequestResponse
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator.Response
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignRequest
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmSignPayload
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmTypedMessage
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.approved
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests.SignWalletConnectRequest

class EvmSignTypedDataRequest(
    private val originAddress: String,
    private val typedMessage: EvmTypedMessage,
    private val sessionRequest: Wallet.Model.SessionRequest
) : SignWalletConnectRequest(sessionRequest) {

    override suspend fun signedResponse(response: Response.Signed): SessionRequestResponse {
        return sessionRequest.approved(response.signature)
    }

    override fun toExternalSignRequest(): ExternalSignRequest {
        val signPayload = EvmSignPayload.SignTypedMessage(typedMessage, originAddress)

        return ExternalSignRequest.Evm(id, signPayload)
    }
}
