package io.novafoundation.nova.runtime.network.binding

import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.data.network.runtime.binding.bindWeightV2
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct

class PerDispatchClassWeight(
    val normal: WeightV2,
    val operational: WeightV2,
    val mandatory: WeightV2
) {
    companion object {

        fun zero(): PerDispatchClassWeight {
            return PerDispatchClassWeight(
                normal = WeightV2.zero(),
                operational = WeightV2.zero(),
                mandatory = WeightV2.zero()
            )
        }

        fun bind(decoded: Any?): PerDispatchClassWeight {
            val asStruct = decoded.castToStruct()
            return PerDispatchClassWeight(
                normal = bindWeightV2(asStruct["normal"]),
                operational = bindWeightV2(asStruct["operational"]),
                mandatory = bindWeightV2(asStruct["mandatory"]),
            )
        }
    }
}

fun PerDispatchClassWeight.total(): WeightV2 {
    return normal + operational + mandatory
}

fun PerDispatchClassWeight?.orZero(): PerDispatchClassWeight {
    return this ?: PerDispatchClassWeight.zero()
}
