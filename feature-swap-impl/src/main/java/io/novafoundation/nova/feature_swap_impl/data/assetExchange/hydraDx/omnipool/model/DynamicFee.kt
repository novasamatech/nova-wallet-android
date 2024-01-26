package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model

import io.novafoundation.nova.common.data.network.runtime.binding.bindPermill
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.Perbill
import io.novafoundation.nova.common.utils.constant
import io.novafoundation.nova.common.utils.decoded
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Module

class DynamicFee(
    val assetFee: Perbill,
    val protocolFee: Perbill
)

fun bindDynamicFee(decoded: Any): DynamicFee {
    val asStruct = decoded.castToStruct()

    return DynamicFee(
        assetFee = bindPermill(asStruct["assetFee"]),
        protocolFee = bindPermill(asStruct["protocolFee"]),
    )
}

class FeeParams(
    val minFee: Perbill,
)

fun bindFeeParams(decoded: Any?): FeeParams {
    val asStruct = decoded.castToStruct()

    return FeeParams(
        minFee = bindPermill(asStruct["minFee"]),
    )
}

fun Module.feeParamsConstant(name: String, runtimeSnapshot: RuntimeSnapshot): FeeParams {
    return bindFeeParams(constant(name).decoded(runtimeSnapshot))
}
