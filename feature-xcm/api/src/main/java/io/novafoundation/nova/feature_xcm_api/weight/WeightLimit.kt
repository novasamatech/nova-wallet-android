package io.novafoundation.nova.feature_xcm_api.weight

import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.utils.scale.ToDynamicScaleInstance
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum

sealed class WeightLimit : ToDynamicScaleInstance {

    companion object {

        fun zero(): WeightLimit {
            return Limited(WeightV2.zero())
        }
    }

    data object Unlimited : WeightLimit() {

        override fun toEncodableInstance(): Any? {
            return DictEnum.Entry("Unlimited", null)
        }
    }

    data class Limited(val weightV2: WeightV2) : WeightLimit() {

        override fun toEncodableInstance(): Any? {
            return DictEnum.Entry("Limited", weightV2.toEncodableInstance())
        }
    }
}
