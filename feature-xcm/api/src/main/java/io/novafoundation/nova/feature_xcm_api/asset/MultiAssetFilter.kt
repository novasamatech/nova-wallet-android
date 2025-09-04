package io.novafoundation.nova.feature_xcm_api.asset

import io.novafoundation.nova.common.utils.scale.ToDynamicScaleInstance
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum

sealed class MultiAssetFilter : ToDynamicScaleInstance {

    sealed class Wild : MultiAssetFilter() {

        object All : Wild() {

            override fun toEncodableInstance(): Any {
                return DictEnum.Entry(
                    name = "Wild",
                    value = DictEnum.Entry(
                        name = "All",
                        value = null
                    )
                )
            }
        }
    }
}
