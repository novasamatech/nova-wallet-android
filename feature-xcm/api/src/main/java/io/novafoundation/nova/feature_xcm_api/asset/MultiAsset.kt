package io.novafoundation.nova.feature_xcm_api.asset;

import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.common.utils.scale.ToDynamicScaleInstance
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.feature_xcm_api.versions.VersionedToDynamicScaleInstance
import io.novafoundation.nova.feature_xcm_api.versions.VersionedXcm
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum

class MultiAsset(
    val id: MultiAssetId,
    val fungibility: Fungibility,
): VersionedToDynamicScaleInstance {

    companion object;


    sealed class Fungibility : ToDynamicScaleInstance {

        class Fungible(val amount: BalanceOf) : Fungibility() {

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

fun MultiAsset.Companion.from(
    multiLocation: RelativeMultiLocation,
    amount: BalanceOf
) = MultiAsset(
    id = MultiAssetId(multiLocation),
    fungibility = MultiAsset.Fungibility.Fungible(amount)
)

@JvmInline
value class MultiAssets(val value: List<MultiAsset>) : VersionedToDynamicScaleInstance {

    constructor(vararg assets: MultiAsset) : this(assets.toList())

    override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
        return value.map { it.toEncodableInstance(xcmVersion) }
    }
}

typealias VersionedMultiAsset = VersionedXcm<MultiAsset>
typealias VersionedMultiAssets = VersionedXcm<MultiAssets>
