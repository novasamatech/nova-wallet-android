package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.common.utils.argument
import io.novafoundation.nova.common.utils.requireActualType
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_xcm_api.message.VersionedXcmMessage
import io.novafoundation.nova.feature_xcm_api.versions.toEncodableInstance
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.Type
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.NumberType
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.call
import io.novasama.substrate_sdk_android.runtime.metadata.module

fun ExtrinsicBuilder.xcmExecute(
    message: VersionedXcmMessage,
    maxWeight: Weight,
): ExtrinsicBuilder {
    return call(
        moduleName = runtime.metadata.xcmPalletName(),
        callName = "execute",
        arguments = mapOf(
            "message" to message.toEncodableInstance(),
            "max_weight" to runtime.prepareWeightForEncoding(maxWeight)
        )
    )
}

private fun RuntimeSnapshot.prepareWeightForEncoding(weight: Weight): Any {
    val moduleName = metadata.xcmPalletName()

    val weightArgumentType = metadata.module(moduleName)
        .call("execute")
        .argument("max_weight")
        .requireActualType()

    return when {
        weightArgumentType.isWeightV1() -> weight
        else -> weight.encodeWeightV2()
    }
}

private fun Weight.encodeWeightV2(): Struct.Instance {
    return structOf("refTime" to this, "proofSize" to Balance.ZERO)
}

private fun Type<*>.isWeightV1(): Boolean {
    return this is NumberType
}
