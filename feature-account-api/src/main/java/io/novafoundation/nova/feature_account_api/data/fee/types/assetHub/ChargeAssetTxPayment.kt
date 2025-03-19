package io.novafoundation.nova.feature_account_api.data.fee.types.assetHub

import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.signedExtensionOrNull
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.bindMultiLocation
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.Type
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHex
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Extrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.SignedExtensionId
import io.novasama.substrate_sdk_android.runtime.metadata.SignedExtensionValue
import java.math.BigInteger

const val CHARGE_ASSET_TX_PAYMENT_ID: SignedExtensionId = "ChargeAssetTxPayment"

fun ExtrinsicBuilder.chargeAssetTxPayment(assetId: Any?, tip: BigInteger = BigInteger.ZERO) {
    val extensionValue = assetTxPaymentPayload(assetId, tip)

    signedExtension(
        id = CHARGE_ASSET_TX_PAYMENT_ID,
        value = SignedExtensionValue(includedInExtrinsic = extensionValue)
    )
}

fun Extrinsic.Instance.findChargeAssetTxPayment(): ChargeAssetTxPaymentValue? {
    val value = signature?.signedExtras?.get(CHARGE_ASSET_TX_PAYMENT_ID) ?: return null
    return ChargeAssetTxPaymentValue.bind(value)
}

fun RuntimeSnapshot.decodeCustomTxPaymentId(assetIdHex: String): Any? {
    val chargeAssetTxPaymentType = metadata.extrinsic.signedExtensionOrNull(CHARGE_ASSET_TX_PAYMENT_ID) ?: return null
    val type = chargeAssetTxPaymentType.includedInExtrinsic!!
    val assetIdType = type.cast<Struct>().get<Type<*>>("assetId")!!

    return assetIdType.fromHex(this, assetIdHex)}

class ChargeAssetTxPaymentValue(
    val tip: BalanceOf,
    val assetId: MultiLocation?
) {

    companion object {

        fun bind(decoded: Any?): ChargeAssetTxPaymentValue {
            val asStruct = decoded.castToStruct()
            return ChargeAssetTxPaymentValue(
                tip = bindNumber(asStruct["tip"]),
                assetId = asStruct.get<Any?>("assetId")?.let(::bindMultiLocation)
            )
        }
    }
}

private fun assetTxPaymentPayload(assetId: Any?, tip: BigInteger = BigInteger.ZERO): Any {
    return structOf(
        "tip" to tip,
        "assetId" to assetId
    )
}
