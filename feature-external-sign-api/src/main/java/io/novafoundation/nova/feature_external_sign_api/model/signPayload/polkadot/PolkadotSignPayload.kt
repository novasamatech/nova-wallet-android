package io.novafoundation.nova.feature_external_sign_api.model.signPayload.polkadot

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class PolkadotSignPayload : Parcelable {

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
        val metadataHash: String?,
        val withSignedTransaction: Boolean?,
        val signedExtensions: List<String>,
        val assetId: String?,
        val version: Int
    ) : PolkadotSignPayload()

    @Parcelize
    class Raw(
        val data: String,
        override val address: String,
        val type: String?
    ) : PolkadotSignPayload()
}

fun PolkadotSignPayload.maybeSignExtrinsic(): PolkadotSignPayload.Json? = this as? PolkadotSignPayload.Json

fun PolkadotSignPayload.genesisHash(): String? = (this as? PolkadotSignPayload.Json)?.genesisHash
