package io.novafoundation.nova.feature_wallet_connect_impl.domain.session

import com.google.gson.Gson
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Wallet.Model.SessionRequest.JSONRPCRequest
import com.walletconnect.web3.wallet.client.Wallet.Params.SessionRequestResponse
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.polkadot.PolkadotSignPayload
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.polkadot.PolkadotSignerResult
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.WalletConnectError
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.approved
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.failed
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.rejected
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.KnownSessionRequest.Params.Polkadot

class KnownSessionRequest(val id: String, val params: Params, val sessionRequest: Wallet.Model.SessionRequest) {

    sealed class Params {

        sealed class Polkadot : Params() {

            class SignTransaction(val address: String, val transactionPayload: PolkadotSignPayload.Json) : Polkadot()

            class SignMessage(val address: String, val message: String) : Polkadot()
        }
    }
}

interface KnownSessionRequestProcessor {

    fun parseKnownRequest(sessionRequest: Wallet.Model.SessionRequest): KnownSessionRequest

    fun prepareResponse(request: KnownSessionRequest, response: ExternalSignCommunicator.Response): SessionRequestResponse
}

class RealKnownSessionRequestProcessor(
    private val gson: Gson
) : KnownSessionRequestProcessor {

    override fun parseKnownRequest(sessionRequest: Wallet.Model.SessionRequest): KnownSessionRequest {
        val request = sessionRequest.request

        val params = when {
            request.method.startsWith("polkadot") -> parsePolkadotRequest(request)

            request.method.startsWith("eth") -> TODO("Ethreum request parsing not yet implemented")

            else -> unknownRequest(request.method)
        }

        return KnownSessionRequest(request.id.toString(), params, sessionRequest)
    }

    override fun prepareResponse(request: KnownSessionRequest, response: ExternalSignCommunicator.Response): SessionRequestResponse {
        return when (response) {
            is ExternalSignCommunicator.Response.Rejected -> request.sessionRequest.rejected()

            is ExternalSignCommunicator.Response.Sent -> when (request.params) {
                is Polkadot -> error("Polkadot WC protocol does not support sending txs")
            }

            is ExternalSignCommunicator.Response.Signed -> {
                val responseJson = when (request.params) {
                    is Polkadot -> preparePolkadotSignResponse(response)
                }

                request.sessionRequest.approved(responseJson)
            }

            is ExternalSignCommunicator.Response.SigningFailed -> request.sessionRequest.failed(
                WalletConnectError.GENERAL_FAILURE
            )
        }
    }

    private fun preparePolkadotSignResponse(signResponse: ExternalSignCommunicator.Response.Signed): String {
        val response = PolkadotSignerResult(signResponse.requestId, signResponse.signature)

        return gson.toJson(response)
    }

    private fun parsePolkadotRequest(request: JSONRPCRequest): KnownSessionRequest.Params {
        return when (request.method) {
            "polkadot_signTransaction" -> gson.fromJson<Polkadot.SignTransaction>(request.params)

            "polkadot_signMessage" -> gson.fromJson<Polkadot.SignMessage>(request.params)

            else -> unknownRequest(request.method)
        }
    }

    private fun unknownRequest(method: String): Nothing = error("Unknown session request: $method")
}
