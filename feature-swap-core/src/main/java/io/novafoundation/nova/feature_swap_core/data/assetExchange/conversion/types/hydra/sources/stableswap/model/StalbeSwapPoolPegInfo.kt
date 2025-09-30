package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap.model

import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import java.math.BigInteger

class StalbeSwapPoolPegInfo(
    val current: List<List<BigInteger>>
)

fun bindPoolPegInfo(decoded: Any?): StalbeSwapPoolPegInfo {
    val asStruct = decoded.castToStruct()
    return StalbeSwapPoolPegInfo(
        current = bindList(asStruct["current"]) { item ->
            bindList(item, ::bindNumber)
        }
    )
}
