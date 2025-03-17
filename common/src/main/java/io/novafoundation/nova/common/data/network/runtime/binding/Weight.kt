package io.novafoundation.nova.common.data.network.runtime.binding

import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import java.math.BigInteger

typealias Weight = BigInteger

data class WeightV2(val refTime: BigInteger, val proofSize: BigInteger)

fun bindWeight(decoded: Any?): Weight {
    return when (decoded) {
        // weight v1
        is BalanceOf -> decoded

        // weight v2
        is Struct.Instance -> bindWeightV2(decoded).refTime

        else -> incompatible()
    }
}

fun bindWeightV2(decoded: Any?): WeightV2 {
    val asStruct = decoded.castToStruct()

    return WeightV2(
        refTime = bindNumber(asStruct["refTime"]),
        proofSize = bindNumber(asStruct["proofSize"])
    )
}
