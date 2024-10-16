package io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests.polkadot

import android.content.Context
import com.google.gson.Gson
import com.walletconnect.web3.wallet.client.Wallet
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignRequest
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.polkadot.PolkadotSignPayload
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.polkadot.PolkadotSignerResult
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.approved
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests.SignWalletConnectRequest

class PolkadotSignRequest(
    private val gson: Gson,
    private val polkadotSignPayload: PolkadotSignPayload,
    private val sessionRequest: Wallet.Model.SessionRequest,
    context: Context
) : SignWalletConnectRequest(sessionRequest, context) {

    override suspend fun signedResponse(response: ExternalSignCommunicator.Response.Signed): Wallet.Params.SessionRequestResponse {
        val responseData = PolkadotSignerResult(id, signature = response.signature, response.modifiedTransaction)
        val responseJson = gson.toJson(responseData)

        return sessionRequest.approved(responseJson)
    }

    override fun toExternalSignRequest(): ExternalSignRequest {
        return ExternalSignRequest.Polkadot(id, polkadotSignPayload)
    }
}
