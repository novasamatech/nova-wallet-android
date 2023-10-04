package io.novafoundation.nova.runtime.multiNetwork.chain.model

import io.novafoundation.nova.runtime.ext.palletNameOrDefault
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHex
import jp.co.soramitsu.fearless_utils.runtime.metadata.call
import jp.co.soramitsu.fearless_utils.runtime.metadata.module
import java.math.BigInteger

sealed interface StatemineAssetId {

    @JvmInline
    value class ScaleEncoded(val scaleHex: String) : StatemineAssetId

    @JvmInline
    value class Number(val value: BigInteger) : StatemineAssetId
}

fun StatemineAssetId.asNumberOrNull(): BigInteger? {
    return (this as? StatemineAssetId.Number)?.value
}


fun StatemineAssetId.asNumberOrThrow(): BigInteger {
    return (this as StatemineAssetId.Number).value
}

fun Chain.Asset.Type.Statemine.prepareIdForEncoding(runtimeSnapshot: RuntimeSnapshot): Any? {
    return when(val id = id) {
        is StatemineAssetId.Number -> id.value
        is StatemineAssetId.ScaleEncoded -> {
            val transferCall = runtimeSnapshot.metadata.module(palletNameOrDefault()).call("transfer")
            val assetIdType = transferCall.arguments.first().type!!

            assetIdType.fromHex(runtimeSnapshot, id.scaleHex)
        }
    }
}
