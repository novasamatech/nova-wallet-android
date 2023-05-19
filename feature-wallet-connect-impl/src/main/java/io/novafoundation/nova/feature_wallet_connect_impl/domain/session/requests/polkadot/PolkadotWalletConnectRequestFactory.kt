package io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests.polkadot

import com.google.gson.Gson
import com.walletconnect.web3.wallet.client.Wallet.Model.SessionRequest
import io.novafoundation.nova.caip.caip2.Caip2Parser
import io.novafoundation.nova.caip.caip2.identifier.Caip2Identifier
import io.novafoundation.nova.caip.caip2.parseCaip2OrThrow
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.polkadot.PolkadotSignPayload
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.WalletConnectError
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests.WalletConnectRequest

class PolkadotWalletConnectRequestFactory(
    private val gson: Gson,
    private val caip2Parser: Caip2Parser
) : WalletConnectRequest.Factory {

    override fun create(sessionRequest: SessionRequest): WalletConnectRequest? {
        val request = sessionRequest.request

        return when (request.method) {
            "polkadot_signTransaction" -> parseSignTransactionRequest(sessionRequest)

            "polkadot_signMessage" -> parseSignMessageRequest(sessionRequest)

            else -> null
        }
    }

    private fun parseSignTransactionRequest(sessionRequest: SessionRequest): WalletConnectRequest {
        val signTxPayload = gson.fromJson<SignTransaction>(sessionRequest.request.params)

        val caip2FromPayload = Caip2Identifier.Polkadot(signTxPayload.transactionPayload.genesisHash)
        val caip2FromChainId = caip2Parser.parseCaip2OrThrow(requireNotNull(sessionRequest.chainId))

        if (caip2FromChainId != caip2FromPayload) throw WalletConnectError.CHAIN_MISMATCH

        return PolkadotSignRequest(
            gson = gson,
            polkadotSignPayload = signTxPayload.transactionPayload,
            sessionRequest = sessionRequest
        )
    }

    private fun parseSignMessageRequest(sessionRequest: SessionRequest): WalletConnectRequest {
        val signMessagePayload = gson.fromJson<SignMessage>(sessionRequest.request.params)

        return PolkadotSignRequest(
            gson = gson,
            polkadotSignPayload = PolkadotSignPayload.Raw(
                data = signMessagePayload.message,
                address = signMessagePayload.address,
                type = null
            ),
            sessionRequest = sessionRequest
        )
    }
}

private class SignTransaction(val transactionPayload: PolkadotSignPayload.Json)

private class SignMessage(val address: String, val message: String)
