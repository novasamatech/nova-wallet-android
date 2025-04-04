package io.novafoundation.nova.feature_xcm_api.weight

import io.novafoundation.nova.common.utils.scale.ToDynamicScaleInstance
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum

sealed class WeightLimit : ToDynamicScaleInstance {

    object Unlimited : WeightLimit() {

        override fun toEncodableInstance(): Any? {
            return DictEnum.Entry("Unlimited", null)
        }
    }
}
