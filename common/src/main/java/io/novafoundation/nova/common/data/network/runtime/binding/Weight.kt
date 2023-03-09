package io.novafoundation.nova.common.data.network.runtime.binding

import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import java.math.BigInteger

typealias Weight = BigInteger

fun bindWeight(decoded: Any?): Weight {
    return when (decoded) {
        // weight v1
        is BalanceOf -> decoded

        // weight v2
        is Struct.Instance -> bindNumber(decoded["refTime"])

        else -> incompatible()
    }
}
