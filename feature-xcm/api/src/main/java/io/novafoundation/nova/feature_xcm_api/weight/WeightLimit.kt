package io.novafoundation.nova.feature_xcm_api.weight

import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.data.network.runtime.binding.bindWeightV2
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.common.utils.scale.ToDynamicScaleInstance
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum

sealed class WeightLimit : ToDynamicScaleInstance {

    companion object {

        fun bind(value: Any?): WeightLimit {
            val asEnum = value.castToDictEnum()
            return when (val name = asEnum.name) {
                "Unlimited" -> Unlimited
                "Limited" -> Limited.bind(asEnum.value)
                else -> incompatible("Unknown WeightLimit variant: $name")
            }
        }

        fun zero(): WeightLimit {
            return Limited(WeightV2.zero())
        }
    }

    object Unlimited : WeightLimit() {

        override fun toEncodableInstance(): Any? {
            return DictEnum.Entry("Unlimited", null)
        }
    }

    class Limited(val weight: WeightV2) : WeightLimit() {

        companion object {

            fun bind(value: Any?): Limited {
                return Limited(bindWeightV2(value))
            }
        }

        override fun toEncodableInstance(): Any? {
            return DictEnum.Entry("Limited", weight.toEncodableInstance())
        }
    }
}
