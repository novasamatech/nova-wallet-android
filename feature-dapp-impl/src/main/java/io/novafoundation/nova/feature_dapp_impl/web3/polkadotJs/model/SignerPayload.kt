package io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model

import android.os.Parcelable
import com.google.gson.Gson
import io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi.ConfirmTxRequest
import kotlinx.android.parcel.Parcelize

@Parcelize
class PolkadotJsSignPayload(val signerPayload: SignerPayload) : ConfirmTxRequest.Payload {

    override val address: String
        get() = signerPayload.address

    override val chainId: String?
        get() = signerPayload.maybeSignExtrinsic()?.genesisHash
}

sealed class SignerPayload : Parcelable {

    abstract val address: String

    @Parcelize
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
        val signedExtensions: List<String>,
        val version: Int
    ) : SignerPayload()

    @Parcelize
    class Raw(
        val data: String,
        override val address: String,
        val type: String?
    ) : SignerPayload()
}

fun SignerPayload.maybeSignExtrinsic() = this as? SignerPayload.Json?

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
