package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model

import io.novafoundation.nova.common.data.network.runtime.binding.bindPermillFraction
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.constant
import io.novafoundation.nova.common.utils.decoded
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module

class DynamicFee(
    val assetFee: Fraction,
    val protocolFee: Fraction
)

fun bindDynamicFee(decoded: Any): DynamicFee {
    val asStruct = decoded.castToStruct()

    return DynamicFee(
        assetFee = bindPermillFraction(asStruct["assetFee"]),
        protocolFee = bindPermillFraction(asStruct["protocolFee"]),
    )
}

class FeeParams(
    val minFee: Fraction,
)

fun bindFeeParams(decoded: Any?): FeeParams {
    val asStruct = decoded.castToStruct()

    return FeeParams(
        minFee = bindPermillFraction(asStruct["minFee"]),
    )
}

fun Module.feeParamsConstant(name: String, runtimeSnapshot: RuntimeSnapshot): FeeParams {
    return bindFeeParams(constant(name).decoded(runtimeSnapshot))
}
