package io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests.evm

import android.content.Context
import com.walletconnect.web3.wallet.client.Wallet
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignRequest
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmPersonalSignMessage
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmSignPayload
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.approved
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests.SignWalletConnectRequest

class EvmPersonalSignRequest(
    private val originAddress: String,
    private val message: EvmPersonalSignMessage,
    private val sessionRequest: Wallet.Model.SessionRequest,
    context: Context
) : SignWalletConnectRequest(sessionRequest, context) {

    override suspend fun signedResponse(response: ExternalSignCommunicator.Response.Signed): Wallet.Params.SessionRequestResponse {
        return sessionRequest.approved(response.signature)
    }

    override fun toExternalSignRequest(): ExternalSignRequest {
        val signPayload = EvmSignPayload.PersonalSign(message, originAddress)

        return ExternalSignRequest.Evm(id, signPayload)
    }
}
