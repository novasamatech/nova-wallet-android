package io.novafoundation.nova.feature_xcm_api.weight

import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.utils.scale.ToDynamicScaleInstance
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum

sealed class WeightLimit : ToDynamicScaleInstance {

    object Unlimited : WeightLimit() {

        override fun toEncodableInstance(): Any? {
            return DictEnum.Entry("Unlimited", null)
        }
    }

    class Limited(val weightV2: WeightV2): WeightLimit() {

        constructor(refTime: Int, proofSize: Int): this(WeightV2(refTime.toBigInteger(), proofSize.toBigInteger()))

        override fun toEncodableInstance(): Any? {
            return DictEnum.Entry("Limited", weightV2.toEncodableInstance())
        }
    }
}
