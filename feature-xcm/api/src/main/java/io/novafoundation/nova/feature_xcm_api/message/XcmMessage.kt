package io.novafoundation.nova.feature_xcm_api.message

import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.feature_xcm_api.asset.MultiAsset
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetFilter
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssets
import io.novafoundation.nova.feature_xcm_api.versions.VersionedToDynamicScaleInstance
import io.novafoundation.nova.feature_xcm_api.versions.VersionedXcm
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.weight.WeightLimit
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import java.math.BigInteger


@JvmInline
value class XcmMessage(val instructions: List<XcmInstruction>): VersionedToDynamicScaleInstance {

    override fun toEncodableInstance(xcmVersion: XcmVersion): Any? {
        return instructions.map { it.toEncodableInstance(xcmVersion) }
    }
}

sealed class XcmInstruction : VersionedToDynamicScaleInstance {

    class WithdrawAsset(val assets: MultiAssets) : XcmInstruction() {

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any? {
            return DictEnum.Entry(
                name = "WithdrawAsset",
                value = assets.toEncodableInstance(xcmVersion)
            )
        }
    }

    class DepositAsset(
        val assets: MultiAssetFilter,
        val maxAssets: BigInteger,
        val beneficiary: RelativeMultiLocation
    ) : XcmInstruction() {

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any? {
           return DictEnum.Entry(
               name = "DepositAsset",
               value = structOf(
                   "assets" to assets.toEncodableInstance(),
                   "max_assets" to maxAssets,
                   "beneficiary" to beneficiary.toEncodableInstance(xcmVersion)
               )
           )
        }
    }

    class BuyExecution(val fees: MultiAsset, val weightLimit: WeightLimit) : XcmInstruction() {

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
            return DictEnum.Entry(
                name = "BuyExecution",
                value = structOf(
                    "fees" to fees.toEncodableInstance(xcmVersion),
                    // xcm v2 always uses v1 weights
                    "weight_limit" to weightLimit.toEncodableInstance()
                )
            )
        }
    }

    object ClearOrigin : XcmInstruction() {

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
            return DictEnum.Entry(
                name = "ClearOrigin",
                value = null
            )
        }
    }

    class ReserveAssetDeposited(val assets: MultiAssets) : XcmInstruction() {

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
            return DictEnum.Entry(
                name = "ReserveAssetDeposited",
                value = assets.toEncodableInstance(xcmVersion)
            )
        }
    }

    class ReceiveTeleportedAsset(val assets: MultiAssets) : XcmInstruction() {
        override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
            return DictEnum.Entry(
                name = "ReceiveTeleportedAsset",
                value = assets.toEncodableInstance(xcmVersion)
            )
        }
    }

    class DepositReserveAsset(
        val assets: MultiAssetFilter,
        val maxAssets: BigInteger,
        val dest: RelativeMultiLocation,
        val xcm: XcmMessage
    ) : XcmInstruction() {
        override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
            return DictEnum.Entry(
                name = "DepositReserveAsset",
                value = structOf(
                    "assets" to assets.toEncodableInstance(),
                    "max_assets" to maxAssets,
                    "dest" to dest.toEncodableInstance(xcmVersion),
                    "xcm" to xcm.toEncodableInstance(xcmVersion)
                )
            )
        }
    }
}

typealias VersionedXcmMessage = VersionedXcm<XcmMessage>
