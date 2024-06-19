package io.novafoundation.nova.hydra_dx_math

import io.novafoundation.nova.common.utils.atLeastZero
import java.math.BigInteger

object HydraDxMathConversions {

    fun String.fromBridgeResultToBalance(): BigInteger? {
        return if (this == "-1") null else toBigInteger().atLeastZero()
    }
}
