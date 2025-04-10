package io.novafoundation.nova.runtime.network.binding

import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.data.network.runtime.binding.bindWeightV2
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct

class BlockWeightLimits(
    val maxBlock: WeightV2,
    val perClass: PerClassLimits
) {

    companion object {

        fun bind(decoded: Any?): BlockWeightLimits {
            val asStruct = decoded.castToStruct()
            return BlockWeightLimits(
                maxBlock = bindWeightV2(asStruct["maxBlock"]),
                perClass = PerClassLimits.bind(asStruct["perClass"])
            )
        }
    }

    class PerClassLimits(
        val normal: ClassLimits
    ) {

        companion object {

            fun bind(decoded: Any?): PerClassLimits {
                val asStruct = decoded.castToStruct()
                return PerClassLimits(
                    normal = ClassLimits.bind(asStruct["normal"])
                )
            }
        }
    }

    class ClassLimits(
        val maxExtrinsic: WeightV2,
        val maxTotal: WeightV2
    ) {

        companion object {

            fun bind(decoded: Any?): ClassLimits {
                val asStruct = decoded.castToStruct()
                return ClassLimits(
                    maxExtrinsic = bindWeightOrMax(asStruct["maxExtrinsic"]),
                    maxTotal = bindWeightOrMax(asStruct["maxTotal"])
                )
            }

            private fun bindWeightOrMax(decoded: Any?): WeightV2 {
                return if (decoded != null) {
                    bindWeightV2(decoded)
                } else {
                    WeightV2.max()
                }
            }
        }
    }
}
