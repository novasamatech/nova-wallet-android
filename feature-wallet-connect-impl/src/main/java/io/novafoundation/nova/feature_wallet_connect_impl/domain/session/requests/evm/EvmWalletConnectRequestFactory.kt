package io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests.evm

import android.content.Context
import com.google.gson.Gson
import com.walletconnect.web3.wallet.client.Wallet
import io.novafoundation.nova.caip.caip2.Caip2Parser
import io.novafoundation.nova.caip.caip2.identifier.Caip2Identifier
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.feature_external_sign_api.domain.sign.evm.EvmTypedMessageParser
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmPersonalSignMessage
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmTransaction
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm.EvmTypedMessage
import io.novafoundation.nova.feature_wallet_connect_impl.data.model.evm.WalletConnectEvmTransaction
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests.WalletConnectRequest

class EvmWalletConnectRequestFactory(
    private val gson: Gson,
    private val caip2Parser: Caip2Parser,
    private val typedMessageParser: EvmTypedMessageParser,
    private val context: Context
) : WalletConnectRequest.Factory {

    override fun create(sessionRequest: Wallet.Model.SessionRequest): WalletConnectRequest? {
        val request = sessionRequest.request

        return when (request.method) {
            "eth_sendTransaction" -> parseEvmSendTx(sessionRequest, sessionRequest.eipChainId())

            "eth_signTransaction" -> parseEvmSignTx(sessionRequest, sessionRequest.eipChainId())

            "eth_signTypedData" -> parseEvmSignTypedMessage(sessionRequest)

            "personal_sign" -> parsePersonalSign(sessionRequest)

            else -> null
        }
    }

    private fun parsePersonalSign(sessionRequest: Wallet.Model.SessionRequest): WalletConnectRequest {
        val (message, address) = gson.fromJson<List<String>>(sessionRequest.request.params)
        val personalSignMessage = EvmPersonalSignMessage(message)

        return EvmPersonalSignRequest(address, personalSignMessage, sessionRequest, context)
    }

    private fun parseEvmSignTypedMessage(sessionRequest: Wallet.Model.SessionRequest): WalletConnectRequest {
        val (address, typedMessage) = parseEvmSignTypedDataParams(sessionRequest.request.params)

        return EvmSignTypedDataRequest(address, typedMessage, sessionRequest, context)
    }

    private fun parseEvmSendTx(sessionRequest: Wallet.Model.SessionRequest, chainId: Int): WalletConnectRequest {
        val transaction = parseStructTransaction(sessionRequest.request.params)

        return EvmSendTransactionRequest(transaction, chainId, sessionRequest, context)
    }

    private fun parseEvmSignTx(sessionRequest: Wallet.Model.SessionRequest, chainId: Int): WalletConnectRequest {
        val transaction = parseStructTransaction(sessionRequest.request.params)

        return EvmSignTransactionRequest(transaction, chainId, sessionRequest, context)
    }

    private fun Wallet.Model.SessionRequest.eipChainId(): Int {
        return chainId?.let(::extractEvmChainId)
            ?: error("No chain id supplied for ${request.method}")
    }

    private fun parseStructTransaction(params: String): EvmTransaction.Struct {
        val parsed: WalletConnectEvmTransaction = parseSingleEvmParameter(params)

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

    private fun parseEvmSignTypedDataParams(params: String): Pair<String, EvmTypedMessage> {
        // params = ["addressParam", structuredDataObject]
        val (addressParam, structuredData) = params.removeSurrounding("[", "]").split(',', limit = 2)
        val address = addressParam.removeSurrounding("\"")

        val evmTypedMessage = typedMessageParser.parseEvmTypedMessage(structuredData)

        return address to evmTypedMessage
    }

    private inline fun <reified T : List<I>, reified I> parseSingleEvmParameter(params: String): I {
        // gson.fromJson<List<I>>(params) does not work even with inlining - gson ignores inner list types and creates hash map instead
        val parsed = gson.fromJson<T>(params)

        return parsed.first()
    }

    private fun extractEvmChainId(caip2: String): Int? {
        return caip2Parser.parseCaip2(caip2)
            .getOrNull()
            ?.castOrNull<Caip2Identifier.Eip155>()
            ?.chainId?.toInt()
    }
}
