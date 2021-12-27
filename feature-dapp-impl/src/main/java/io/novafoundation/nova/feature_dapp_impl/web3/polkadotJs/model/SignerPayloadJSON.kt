package io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model

import android.os.Parcelable
import com.google.gson.Gson
import kotlinx.android.parcel.Parcelize

@Parcelize
class SignerPayloadJSON(
    val address: String,
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
) : Parcelable

fun mapRawPayloadToSignerPayloadJSON(
    raw: Any?,
    gson: Gson
): SignerPayloadJSON? {
    val tree = gson.toJsonTree(raw)

    return runCatching {
        gson.fromJson(tree, SignerPayloadJSON::class.java)
    }.getOrNull()
}
