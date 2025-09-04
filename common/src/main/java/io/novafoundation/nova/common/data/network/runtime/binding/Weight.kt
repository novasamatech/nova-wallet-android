package io.novafoundation.nova.common.data.network.runtime.binding

import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import java.math.BigInteger

typealias Weight = BigInteger

fun bindWeight(decoded: Any?): Weight {
    return when (decoded) {
        // weight v1
        is BalanceOf -> decoded

        // weight v2
        is Struct.Instance -> bindWeightV2(decoded).refTime

        else -> incompatible()
    }
}
