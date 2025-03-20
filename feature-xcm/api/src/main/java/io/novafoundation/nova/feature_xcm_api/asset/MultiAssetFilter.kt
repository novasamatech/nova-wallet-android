package io.novafoundation.nova.feature_xcm_api.asset

import io.novafoundation.nova.common.utils.scale.ToDynamicScaleInstance
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum

sealed class MultiAssetFilter : ToDynamicScaleInstance {

    sealed class Wild : MultiAssetFilter() {

        /**
         * Filter to use all assets from the holding register
         *
         * !!! Important !!!
         * Weight of this instruction is bounded by maximum number of assets usable per instruction,
         * which can be 100 in some runtimes.
         * This might result in sever overestimation of instruction weight and thus, the fee.
         *
         * Please use other variations that put explicit desired bound like [AllCounted] whenever possible
         */
        object All : Wild() {

            override fun toString(): String {
                return "All"
            }

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

        /**
         * Filter to use first [assetsCount] assets from the holding register
         */
        data class AllCounted(val assetsCount: Int) : Wild() {

            override fun toEncodableInstance(): Any {
                return DictEnum.Entry(
                    name = "Wild",
                    value = DictEnum.Entry(
                        name = "AllCounted",
                        value = assetsCount.toBigInteger()
                    )
                )
            }
        }
    }
}
