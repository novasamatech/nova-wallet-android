package io.novafoundation.nova.feature_xcm_api.asset

import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.common.utils.scale.ToDynamicScaleInstance
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.feature_xcm_api.versions.VersionedToDynamicScaleInstance
import io.novafoundation.nova.feature_xcm_api.versions.VersionedXcm
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.versions.bindVersionedXcm
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import java.math.BigInteger

data class MultiAsset private constructor(
    val id: MultiAssetId,
    val fungibility: Fungibility,
) : VersionedToDynamicScaleInstance {

    companion object {

        fun bind(decodedInstance: Any?, xcmVersion: XcmVersion): MultiAsset {
            val asStruct = decodedInstance.castToStruct()
            return MultiAsset(
                id = bindMultiAssetId(asStruct["id"], xcmVersion),
                fungibility = Fungibility.bind(asStruct["fun"])
            )
        }

        fun from(
            multiLocation: RelativeMultiLocation,
            amount: BalanceOf
        ): MultiAsset {
            // Substrate doesn't allow zero balance starting from xcm v3
            val positiveAmount = amount.coerceAtLeast(BigInteger.ONE)

            return MultiAsset(
                id = MultiAssetId(multiLocation),
                fungibility = Fungibility.Fungible(positiveAmount)
            )
        }
    }

    sealed class Fungibility : ToDynamicScaleInstance {

        companion object {

            fun bind(decodedInstance: Any?): Fungibility {
                val asEnum = decodedInstance.castToDictEnum()

                return when (asEnum.name) {
                    "Fungible" -> Fungible(bindNumber(asEnum.value))
                    else -> incompatible()
                }
            }
        }

        data class Fungible(val amount: BalanceOf) : Fungibility() {

            override fun toEncodableInstance(): Any {
                return DictEnum.Entry(name = "Fungible", value = amount)
            }
        }
    }

    override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
        return structOf(
            "fun" to fungibility.toEncodableInstance(),
            "id" to id.toEncodableInstance(xcmVersion)
        )
    }
}

fun MultiAsset.requireFungible(): MultiAsset.Fungibility.Fungible {
    return fungibility.cast()
}

@JvmInline
value class MultiAssets(val value: List<MultiAsset>) : VersionedToDynamicScaleInstance {

    companion object {

        fun bind(decodedInstance: Any?, xcmVersion: XcmVersion): MultiAssets {
            val assets = bindList(decodedInstance) { MultiAsset.bind(it, xcmVersion) }
            return MultiAssets(assets)
        }

        fun bindVersioned(decodedInstance: Any?): VersionedMultiAssets {
            return bindVersionedXcm(decodedInstance, MultiAssets::bind)
        }
    }

    constructor(vararg assets: MultiAsset) : this(assets.toList())

    override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
        return value.map { it.toEncodableInstance(xcmVersion) }
    }
}

fun List<MultiAsset>.intoMultiAssets(): MultiAssets {
    return MultiAssets(this)
}

fun MultiAsset.intoMultiAssets(): MultiAssets {
    return MultiAssets(listOf(this))
}

typealias VersionedMultiAsset = VersionedXcm<MultiAsset>
typealias VersionedMultiAssets = VersionedXcm<MultiAssets>
