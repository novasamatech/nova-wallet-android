package io.novafoundation.nova.feature_account_api.data.fee.types.assetHub

import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.transactionExtensionOrNull
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.bindMultiLocation
import io.novafoundation.nova.runtime.extrinsic.extensions.ChargeAssetTxPayment
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.Type
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHex
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Extrinsic
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.findExplicitOrNull

fun Extrinsic.Instance.findChargeAssetTxPayment(): ChargeAssetTxPaymentValue? {
    val value = findExplicitOrNull(ChargeAssetTxPayment.ID) ?: return null
    return ChargeAssetTxPaymentValue.bind(value)
}

fun RuntimeSnapshot.decodeCustomTxPaymentId(assetIdHex: String): Any? {
    val chargeAssetTxPaymentType = metadata.extrinsic.transactionExtensionOrNull(ChargeAssetTxPayment.ID) ?: return null
    val type = chargeAssetTxPaymentType.includedInExtrinsic!!
    val assetIdType = type.cast<Struct>().get<Type<*>>("assetId")!!

    return assetIdType.fromHex(this, assetIdHex)
}

class ChargeAssetTxPaymentValue(
    val tip: BalanceOf,
    val assetId: RelativeMultiLocation?
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
