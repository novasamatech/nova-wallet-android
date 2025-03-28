package io.novafoundation.nova.feature_xcm_api.asset

import io.novafoundation.nova.common.data.network.runtime.binding.bindInt
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.feature_xcm_api.versions.VersionedToDynamicScaleInstance
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum

sealed class MultiAssetFilter : VersionedToDynamicScaleInstance {

    companion object {

        fun singleCounted(): Wild.AllCounted {
            return Wild.AllCounted(assetsCount = 1)
        }

        fun bind(value: Any?, xcmVersion: XcmVersion): MultiAssetFilter {
            val enum = value.castToDictEnum()
            return when (val name = enum.name) {
                "Definite" -> Definite.bind(enum.value, xcmVersion)
                "Wild" -> Wild.bind(enum.value)
                else -> incompatible("Unknown MultiAssetFilter variant: $name")
            }
        }
    }

    class Definite(val assets: MultiAssets) : MultiAssetFilter() {

        companion object {
            fun bind(value: Any?, xcmVersion: XcmVersion): Definite {
                return Definite(MultiAssets.bind(value, xcmVersion))
            }
        }

        constructor(asset: MultiAsset) : this(asset.intoMultiAssets())

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any? {
            return DictEnum.Entry("Definite", assets.toEncodableInstance(xcmVersion))
        }
    }

    sealed class Wild : MultiAssetFilter() {

        companion object {
            fun bind(value: Any?): Wild {
                val asEnum = value.castToDictEnum()
                return when (val name = asEnum.name) {
                    "All" -> All
                    "AllCounted" -> AllCounted.bind(asEnum.value)
                    else -> incompatible("Unknown Wild variant: $name")
                }
            }
        }


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

            override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
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

            companion object {

                fun bind(value: Any?): AllCounted {
                    return AllCounted(bindInt(value))
                }
            }

            override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
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
