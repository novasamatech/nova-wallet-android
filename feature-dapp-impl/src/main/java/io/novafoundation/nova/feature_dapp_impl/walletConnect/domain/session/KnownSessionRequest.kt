package io.novafoundation.nova.feature_dapp_impl.walletConnect.domain.session

import com.google.gson.Gson
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Wallet.Model.SessionRequest.JSONRPCRequest
import com.walletconnect.web3.wallet.client.Wallet.Params.SessionRequestResponse
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignCommunicator
import io.novafoundation.nova.feature_dapp_impl.walletConnect.domain.sdk.WalletConnectError
import io.novafoundation.nova.feature_dapp_impl.walletConnect.domain.sdk.approved
import io.novafoundation.nova.feature_dapp_impl.walletConnect.domain.sdk.failed
import io.novafoundation.nova.feature_dapp_impl.walletConnect.domain.sdk.rejected
import io.novafoundation.nova.feature_dapp_impl.walletConnect.domain.session.KnownSessionRequest.Params.Polkadot
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerPayload
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerResult

class KnownSessionRequest(val id: String, val params: Params, val sessionRequest: Wallet.Model.SessionRequest) {

    sealed class Params {

        sealed class Polkadot : Params() {

            class SignTransaction(val address: String, val transactionPayload: SignerPayload.Json) : Polkadot()

            class SignMessage(val address: String, val message: String) : Polkadot()
        }
    }
}

interface KnownSessionRequestProcessor {

    fun parseKnownRequest(request: Wallet.Model.SessionRequest): KnownSessionRequest

    fun prepareResponse(request: KnownSessionRequest, response: DAppSignCommunicator.Response): SessionRequestResponse
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

    override fun prepareResponse(request: KnownSessionRequest, response: DAppSignCommunicator.Response) : SessionRequestResponse {
        return when (response) {
            is DAppSignCommunicator.Response.Rejected -> request.sessionRequest.rejected()

            is DAppSignCommunicator.Response.Sent -> when (request.params) {
                is Polkadot -> error("Polkadot WC protocol does not support sending txs")
            }

            is DAppSignCommunicator.Response.Signed -> {
                val responseJson = when (request.params) {
                    is Polkadot -> preparePolkadotSignResponse(response)
                }

                request.sessionRequest.approved(responseJson)
            }

            is DAppSignCommunicator.Response.SigningFailed -> request.sessionRequest.failed(WalletConnectError.GENERAL_FAILURE)
        }
    }

    private fun preparePolkadotSignResponse(signResponse: DAppSignCommunicator.Response.Signed): String {
        val response = SignerResult(signResponse.requestId, signResponse.signature)

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
