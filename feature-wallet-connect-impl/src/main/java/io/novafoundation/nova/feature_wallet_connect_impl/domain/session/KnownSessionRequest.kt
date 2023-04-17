package io.novafoundation.nova.feature_wallet_connect_impl.domain.session

import com.google.gson.Gson
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Wallet.Model.SessionRequest.JSONRPCRequest
import com.walletconnect.web3.wallet.client.Wallet.Params.SessionRequestResponse
import io.novafoundation.nova.caip.caip2.Caip2Parser
import io.novafoundation.nova.caip.caip2.identifier.Caip2Identifier
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmChainSource
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmSignPayload
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmSignPayload.ConfirmTx
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmTransaction
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.polkadot.PolkadotSignPayload
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.polkadot.PolkadotSignerResult
import io.novafoundation.nova.feature_wallet_connect_impl.data.model.evm.WalletConnectEvmTransaction
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.WalletConnectError
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.approved
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.failed
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.rejected
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.KnownSessionRequest.Params.Evm
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.KnownSessionRequest.Params.Polkadot

class KnownSessionRequest(val id: String, val params: Params, val sessionRequest: Wallet.Model.SessionRequest) {

    sealed class Params {

        sealed class Polkadot : Params() {

            class SignTransaction(val transactionPayload: PolkadotSignPayload.Json) : Polkadot()

            class SignMessage(val address: String, val message: String) : Polkadot()
        }

        class Evm(val payload: EvmSignPayload) : Params()
    }
}

interface KnownSessionRequestProcessor {

    fun parseKnownRequest(sessionRequest: Wallet.Model.SessionRequest): KnownSessionRequest

    fun prepareResponse(request: KnownSessionRequest, response: ExternalSignCommunicator.Response): SessionRequestResponse
}

class RealKnownSessionRequestProcessor(
    private val gson: Gson,
    private val caip2Parser: Caip2Parser,
) : KnownSessionRequestProcessor {

    override fun parseKnownRequest(sessionRequest: Wallet.Model.SessionRequest): KnownSessionRequest {
        val request = sessionRequest.request

        val params = when {
            request.method.startsWith("polkadot") -> parsePolkadotRequest(request)

            request.method.startsWith("eth") -> {
                val chainId = sessionRequest.chainId
                    ?.let(::extractEvmChainId)
                    ?: unknownRequest("No chain id supplied for ${request.method}")

                parseEvmRequest(request, chainId)
            }

            else -> unknownRequest(request.method)
        }

        return KnownSessionRequest(request.id.toString(), params, sessionRequest)
    }

    override fun prepareResponse(request: KnownSessionRequest, response: ExternalSignCommunicator.Response): SessionRequestResponse {
        return when (response) {
            is ExternalSignCommunicator.Response.Rejected -> request.sessionRequest.rejected()

            is ExternalSignCommunicator.Response.Sent -> {
                val responseJson = when (request.params) {
                    is Polkadot -> error("Polkadot WC protocol does not support sending txs")
                    is Evm -> prepareEvmResponse(response)
                }

                request.sessionRequest.approved(responseJson)
            }

            is ExternalSignCommunicator.Response.Signed -> {
                val responseJson = when (request.params) {
                    is Polkadot -> preparePolkadotSignResponse(response)
                    is Evm -> prepareEvmResponse(response)
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

    private fun prepareEvmResponse(signResponse: ExternalSignCommunicator.Response.Signed): String {
        return signResponse.signature
    }

    private fun prepareEvmResponse(signResponse: ExternalSignCommunicator.Response.Sent): String {
        return signResponse.txHash
    }

    private fun parsePolkadotRequest(request: JSONRPCRequest): KnownSessionRequest.Params {
        return when (request.method) {
            "polkadot_signTransaction" -> gson.fromJson<Polkadot.SignTransaction>(request.params)

            "polkadot_signMessage" -> gson.fromJson<Polkadot.SignMessage>(request.params)

            else -> unknownRequest(request.method)
        }
    }

    private fun parseEvmRequest(request: JSONRPCRequest, chainId: Int): KnownSessionRequest.Params {
        val signPayload = when (request.method) {
            "eth_sendTransaction" -> parseEvmConfirmTx(request, chainId, ConfirmTx.Action.SEND)

            "eth_signTransaction" -> parseEvmConfirmTx(request, chainId, ConfirmTx.Action.SIGN)

            else -> unknownRequest(request.method)
        }

        return Evm(signPayload)
    }

    private fun parseEvmConfirmTx(request: JSONRPCRequest, chainId: Int, action: ConfirmTx.Action): EvmSignPayload {
        return parseStructTransaction(request.params).let { transaction ->
            ConfirmTx(
                transaction = transaction,
                originAddress = transaction.from,
                chainSource = EvmChainSource(chainId, EvmChainSource.UnknownChainOptions.MustBeKnown),
                action =action
            )
        }
    }

    private fun unknownRequest(reason: String): Nothing = error("Unknown session request: $reason")

    private fun parseStructTransaction(params: String): EvmTransaction.Struct {
        val parsed: WalletConnectEvmTransaction = parseEvmParameters(params)

        return with(parsed) {
            EvmTransaction.Struct(
                gas = gasLimit,
                gasPrice = gasPrice,
                from = from,
                to = to,
                data = data,
                value = value,
                nonce = nonce
            )
        }
    }

    private inline fun <reified T : List<I>, reified I> parseEvmParameters(params: String): I {
        // gson.fromJson<List<I>>(params) does not work even with inlining - gson ignores inner list types and creates hash map instead
        val parsed = gson.fromJson<T>(params)

        return parsed.first()
    }

    private fun extractEvmChainId(caip2: String): Int? {
        return caip2Parser.parserCaip2(caip2)
            .getOrNull()
            ?.castOrNull<Caip2Identifier.Eip155>()
            ?.chainId?.toInt()
    }
}
