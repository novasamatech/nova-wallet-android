package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.xyk.model

import io.novafoundation.nova.common.data.network.runtime.binding.bindInt
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.utils.constant
import io.novafoundation.nova.common.utils.decoded
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module

class XYKFees(val nominator: Int, val denominator: Int)

fun bindXYKFees(decoded: Any?): XYKFees {
    val (first, second) = decoded.castToList()

    return XYKFees(bindInt(first), bindInt(second))
}

fun Module.poolFeesConstant(runtimeSnapshot: RuntimeSnapshot): XYKFees {
    return bindXYKFees(constant("GetExchangeFee").decoded(runtimeSnapshot))
}
