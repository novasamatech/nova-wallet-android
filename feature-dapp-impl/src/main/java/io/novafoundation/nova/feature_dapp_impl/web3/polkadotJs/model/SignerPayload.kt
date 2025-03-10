package io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model

import com.google.gson.Gson
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.polkadot.PolkadotSignPayload

sealed class SignerPayload {

    abstract val address: String

    class Json(
        override val address: String,
        val blockHash: String,
        val blockNumber: String,
        val era: String,
        val genesisHash: String,
        val method: String,
        val nonce: String,
        val specVersion: String,
        val tip: String,
        val transactionVersion: String,
        val metadataHash: String?,
        val withSignedTransaction: Boolean?,
        val signedExtensions: List<String>,
        val version: Int
    ) : SignerPayload()

    class Raw(
        val data: String,
        override val address: String,
        val type: String?
    ) : SignerPayload()
}

fun mapRawPayloadToSignerPayloadJSON(
    raw: Any?,
    gson: Gson
): SignerPayload.Json? {
    val tree = gson.toJsonTree(raw)

    return runCatching {
        gson.fromJson(tree, SignerPayload.Json::class.java)
    }.getOrNull()
}

fun mapRawPayloadToSignerPayloadRaw(
    raw: Any?,
    gson: Gson
): SignerPayload.Raw? {
    val tree = gson.toJsonTree(raw)

    return runCatching {
        gson.fromJson(tree, SignerPayload.Raw::class.java)
    }.getOrNull()
}

fun mapPolkadotJsSignerPayloadToPolkadotPayload(signerPayload: SignerPayload): PolkadotSignPayload {
    return when (signerPayload) {
        is SignerPayload.Json -> with(signerPayload) {
            PolkadotSignPayload.Json(
                address = address,
                blockHash = blockHash,
                blockNumber = blockNumber,
                era = era,
                genesisHash = genesisHash,
                metadataHash = metadataHash,
                method = method,
                nonce = nonce,
                specVersion = specVersion,
                tip = tip,
                transactionVersion = transactionVersion,
                signedExtensions = signedExtensions,
                withSignedTransaction = withSignedTransaction,
                version = version
            )
        }
        is SignerPayload.Raw -> with(signerPayload) {
            PolkadotSignPayload.Raw(
                data = data,
                address = address,
                type = type
            )
        }
    }
}
