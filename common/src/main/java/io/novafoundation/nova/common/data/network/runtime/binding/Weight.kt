package io.novafoundation.nova.common.data.network.runtime.binding

import io.novafoundation.nova.common.utils.scale.ToDynamicScaleInstance
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.common.utils.times
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import org.bouncycastle.math.ec.WTauNafMultiplier
import java.math.BigInteger

typealias Weight = BigInteger

data class WeightV2(val refTime: BigInteger, val proofSize: BigInteger): ToDynamicScaleInstance {

    companion object {

        fun fromV1(refTime: BigInteger): WeightV2 {
            return WeightV2(refTime, proofSize = BigInteger.ZERO)
        }
    }

    operator fun times(multiplier: Double): WeightV2 {
        return WeightV2(refTime = refTime.times(multiplier), proofSize = proofSize.times(multiplier))
    }

    override fun toEncodableInstance(): Struct.Instance {
        return structOf("refTime" to refTime, "proofSize" to proofSize)
    }
}

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
