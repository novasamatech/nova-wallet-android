package io.novafoundation.nova.common.data.network.runtime.binding

import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.utils.scale.ToDynamicScaleInstance
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.common.utils.times
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import java.math.BigInteger

data class WeightV2(val refTime: BigInteger, val proofSize: BigInteger) : ToDynamicScaleInstance {

    companion object {

        val MAX_DIMENSION = "184467440737090".toBigInteger()

        fun max(): WeightV2 {
            return WeightV2(MAX_DIMENSION, MAX_DIMENSION)
        }

        fun fromV1(refTime: BigInteger): WeightV2 {
            return WeightV2(refTime, proofSize = BigInteger.ZERO)
        }

        fun zero(): WeightV2 {
            return WeightV2(BigInteger.ZERO, BigInteger.ZERO)
        }
    }

    operator fun times(multiplier: Double): WeightV2 {
        return WeightV2(refTime = refTime.times(multiplier), proofSize = proofSize.times(multiplier))
    }

    operator fun plus(other: WeightV2): WeightV2 {
        return WeightV2(refTime + other.refTime, proofSize + other.proofSize)
    }

    operator fun minus(other: WeightV2): WeightV2 {
        return WeightV2(
            refTime = (refTime - other.refTime).atLeastZero(),
            proofSize = (proofSize - other.proofSize).atLeastZero()
        )
    }

    override fun toEncodableInstance(): Struct.Instance {
        return structOf("refTime" to refTime, "proofSize" to proofSize)
    }
}

fun WeightV2.fitsIn(limit: WeightV2): Boolean {
    return refTime <= limit.refTime && proofSize < limit.proofSize
}

fun bindWeightV2(decoded: Any?): WeightV2 {
    return when (decoded) {
        is BalanceOf -> WeightV2.fromV1(decoded)

        is Struct.Instance -> WeightV2(
            refTime = bindNumber(decoded["refTime"]),
            proofSize = bindNumber(decoded["proofSize"])
        )

        else -> incompatible()
    }
}


