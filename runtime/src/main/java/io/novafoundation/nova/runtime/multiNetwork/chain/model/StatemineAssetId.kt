package io.novafoundation.nova.runtime.multiNetwork.chain.model

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.runtime.ext.palletNameOrDefault
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.RuntimeType
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHex
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.toHexUntyped
import jp.co.soramitsu.fearless_utils.runtime.metadata.callOrNull
import jp.co.soramitsu.fearless_utils.runtime.metadata.moduleOrNull
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

fun StatemineAssetId.asScaleEncodedOrThrow(): String {
    return (this as StatemineAssetId.ScaleEncoded).scaleHex
}

fun StatemineAssetId.asScaleEncodedOrNull(): String? {
    return (this as? StatemineAssetId.ScaleEncoded)?.scaleHex
}

fun StatemineAssetId.isScaleEncoded(): Boolean {
    return this is StatemineAssetId.ScaleEncoded
}

fun Chain.Asset.Type.Statemine.prepareIdForEncoding(runtimeSnapshot: RuntimeSnapshot): Any? {
    return when (val id = id) {
        is StatemineAssetId.Number -> id.value

        is StatemineAssetId.ScaleEncoded -> {
            val assetIdType = statemineAssetIdScaleType(runtimeSnapshot, palletNameOrDefault())

            assetIdType!!.fromHex(runtimeSnapshot, id.scaleHex)
        }
    }
}

fun Chain.Asset.Type.Statemine.hasSameId(runtimeSnapshot: RuntimeSnapshot, dynamicInstanceId: Any?): Boolean {
    return runCatching {
        when (val id = id) {
            is StatemineAssetId.Number -> id.value == bindNumber(dynamicInstanceId)

            is StatemineAssetId.ScaleEncoded -> {
                val assetIdType = statemineAssetIdScaleType(runtimeSnapshot, palletNameOrDefault())
                val otherScale = assetIdType!!.toHexUntyped(runtimeSnapshot, dynamicInstanceId)

                id.scaleHex == otherScale
            }
        }
    }.getOrDefault(false)
}

fun statemineAssetIdScaleType(runtimeSnapshot: RuntimeSnapshot, palletName: String): RuntimeType<*, *>? {
    val transferCall = runtimeSnapshot.metadata.moduleOrNull(palletName)?.callOrNull("transfer")
    return transferCall?.arguments?.firstOrNull()?.type
}
