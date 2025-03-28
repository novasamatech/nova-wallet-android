package io.novafoundation.nova.feature_xcm_api.message

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.feature_xcm_api.asset.MultiAsset
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetFilter
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssets
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.versions.VersionedToDynamicScaleInstance
import io.novafoundation.nova.feature_xcm_api.versions.VersionedXcm
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.versions.bindVersionedXcm
import io.novafoundation.nova.feature_xcm_api.versions.versionedXcm
import io.novafoundation.nova.feature_xcm_api.weight.WeightLimit
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import java.math.BigInteger

@JvmInline
value class XcmMessage(val instructions: List<XcmInstruction>) : VersionedToDynamicScaleInstance {

    constructor(vararg instructions: XcmInstruction) : this(instructions.toList())

    companion object {

        fun bindKnown(decoded: Any?, xcmVersion: XcmVersion): XcmMessage {
            return bindList(decoded) {
                XcmInstruction.bindKnown(it, xcmVersion)
            }
                .filterNotNull()
                .asXcmMessage()
        }
    }

    override fun toEncodableInstance(xcmVersion: XcmVersion): Any? {
        return instructions.map { it.toEncodableInstance(xcmVersion) }
    }
}

fun List<XcmInstruction>.asXcmMessage(): XcmMessage = XcmMessage(this)

fun List<XcmInstruction>.asVersionedXcmMessage(version: XcmVersion): VersionedXcmMessage = asXcmMessage().versionedXcm(version)

sealed class XcmInstruction : VersionedToDynamicScaleInstance {

    companion object {

        fun bindKnown(decoded: Any?, xcmVersion: XcmVersion): XcmInstruction? {
            val enum = decoded.castToDictEnum()
            return when (enum.name) {
                "WithdrawAsset" -> WithdrawAsset.bind(enum.value, xcmVersion)
                "DepositAsset" -> DepositAsset.bind(enum.value, xcmVersion)
                "BuyExecution" -> BuyExecution.bind(enum.value, xcmVersion)
                "ClearOrigin" -> ClearOrigin
                "ReserveAssetDeposited" -> ReserveAssetDeposited.bind(enum.value, xcmVersion)
                "ReceiveTeleportedAsset" -> ReceiveTeleportedAsset.bind(enum.value, xcmVersion)
                "InitiateReserveWithdraw" -> InitiateReserveWithdraw.bind(enum.value, xcmVersion)
                "TransferReserveAsset" -> TransferReserveAsset.bind(enum.value, xcmVersion)
                "DepositReserveAsset" -> DepositReserveAsset.bind(enum.value, xcmVersion)
                "PayFees" -> PayFees.bind(enum.value, xcmVersion)
                "InitiateTeleport" -> InitiateTeleport.bind(enum.value, xcmVersion)
                else -> {
                    Log.w("XcmInstruction", "Attempting to bind unknown instruction: ${enum.name}")

                    null
                }
            }
        }
    }

    data class WithdrawAsset(val assets: MultiAssets) : XcmInstruction() {

        companion object {

            fun bind(value: Any?, xcmVersion: XcmVersion): WithdrawAsset {
                return WithdrawAsset(MultiAssets.bind(value, xcmVersion))
            }
        }

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any? {
            return DictEnum.Entry(
                name = "WithdrawAsset",
                value = assets.toEncodableInstance(xcmVersion)
            )
        }
    }

    data class DepositAsset(
        val assets: MultiAssetFilter,
        val beneficiary: RelativeMultiLocation
    ) : XcmInstruction() {

        companion object {

            fun bind(value: Any?, xcmVersion: XcmVersion): DepositAsset {
                val struct = value.castToStruct()

                return DepositAsset(
                    assets = MultiAssetFilter.bind(struct["assets"], xcmVersion),
                    beneficiary = RelativeMultiLocation.bind(struct["beneficiary"])
                )
            }
        }

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any? {
            return DictEnum.Entry(
                name = "DepositAsset",
                value = structOf(
                    "assets" to assets.toEncodableInstance(xcmVersion),
                    "beneficiary" to beneficiary.toEncodableInstance(xcmVersion),
                    // Used in XCM V2 and below. We put 1 here since we only support cases for transferring a single asset
                    "max_assets" to BigInteger.ONE,
                )
            )
        }
    }

    data class BuyExecution(val fees: MultiAsset, val weightLimit: WeightLimit) : XcmInstruction() {

        companion object {

            fun bind(value: Any?, xcmVersion: XcmVersion): BuyExecution {
                val struct = value.castToStruct()

                return BuyExecution(
                    fees = MultiAsset.bind(struct["fees"], xcmVersion),
                    weightLimit = WeightLimit.bind(struct["weight_limit"])
                )
            }
        }

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

    data class ReserveAssetDeposited(val assets: MultiAssets) : XcmInstruction() {

        companion object {

            fun bind(value: Any?, xcmVersion: XcmVersion): ReserveAssetDeposited {
                return ReserveAssetDeposited(MultiAssets.bind(value, xcmVersion))
            }
        }

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
            return DictEnum.Entry(
                name = "ReserveAssetDeposited",
                value = assets.toEncodableInstance(xcmVersion)
            )
        }
    }

    data class ReceiveTeleportedAsset(val assets: MultiAssets) : XcmInstruction() {

        companion object {

            fun bind(value: Any?, xcmVersion: XcmVersion): ReceiveTeleportedAsset {
                return ReceiveTeleportedAsset(MultiAssets.bind(value, xcmVersion))
            }
        }

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
            return DictEnum.Entry(
                name = "ReceiveTeleportedAsset",
                value = assets.toEncodableInstance(xcmVersion)
            )
        }
    }

    data class InitiateReserveWithdraw(
        val assets: MultiAssetFilter,
        val reserve: RelativeMultiLocation,
        val xcm: XcmMessage
    ) : XcmInstruction() {

        companion object {

            fun bind(value: Any?, xcmVersion: XcmVersion): InitiateReserveWithdraw {
                val struct = value.castToStruct()

                return InitiateReserveWithdraw(
                    assets = MultiAssetFilter.bind(struct["assets"], xcmVersion),
                    reserve = RelativeMultiLocation.bind(struct["reserve"]),
                    xcm = XcmMessage.bindKnown(struct["xcm"], xcmVersion)
                )
            }
        }

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
            return DictEnum.Entry(
                name = "InitiateReserveWithdraw",
                value = structOf(
                    "assets" to assets.toEncodableInstance(xcmVersion),
                    "reserve" to reserve.toEncodableInstance(xcmVersion),
                    "xcm" to xcm.toEncodableInstance(xcmVersion)
                )
            )
        }
    }

    data class TransferReserveAsset(
        val assets: MultiAssets,
        val dest: RelativeMultiLocation,
        val xcm: XcmMessage
    ) : XcmInstruction() {

        companion object {

            fun bind(value: Any?, xcmVersion: XcmVersion): TransferReserveAsset {
                val struct = value.castToStruct()

                return TransferReserveAsset(
                    assets = MultiAssets.bind(struct["assets"], xcmVersion),
                    dest = RelativeMultiLocation.bind(struct["dest"]),
                    xcm = XcmMessage.bindKnown(struct["xcm"], xcmVersion)
                )
            }
        }

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
            return DictEnum.Entry(
                name = "TransferReserveAsset",
                value = structOf(
                    "assets" to assets.toEncodableInstance(xcmVersion),
                    "dest" to dest.toEncodableInstance(xcmVersion),
                    "xcm" to xcm.toEncodableInstance(xcmVersion)
                )
            )
        }
    }

    data class DepositReserveAsset(
        val assets: MultiAssetFilter,
        val dest: RelativeMultiLocation,
        val xcm: XcmMessage
    ) : XcmInstruction() {

        companion object {

            fun bind(value: Any?, xcmVersion: XcmVersion): DepositReserveAsset {
                val struct = value.castToStruct()

                return DepositReserveAsset(
                    assets = MultiAssetFilter.bind(struct["assets"], xcmVersion),
                    dest = RelativeMultiLocation.bind(struct["dest"]),
                    xcm = XcmMessage.bindKnown(struct["xcm"], xcmVersion)
                )
            }
        }

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
            return DictEnum.Entry(
                name = "DepositReserveAsset",
                value = structOf(
                    "assets" to assets.toEncodableInstance(xcmVersion),
                    // Used in XCM V2 and below. We put 1 here since we only support cases for transferring a single asset
                    "max_assets" to BigInteger.ONE,
                    "dest" to dest.toEncodableInstance(xcmVersion),
                    "xcm" to xcm.toEncodableInstance(xcmVersion)
                )
            )
        }
    }

    data class PayFees(val fees: MultiAsset) : XcmInstruction() {

        companion object {

            fun bind(value: Any?, xcmVersion: XcmVersion): PayFees {
                return PayFees(MultiAsset.bind(value, xcmVersion))
            }
        }

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
            return DictEnum.Entry(
                name = "PayFees",
                value = structOf(
                    "fees" to fees.toEncodableInstance(xcmVersion)
                )
            )
        }
    }

    data class InitiateTeleport(
        val assets: MultiAssetFilter,
        val dest: RelativeMultiLocation,
        val xcm: XcmMessage
    ) : XcmInstruction() {

        companion object {

            fun bind(value: Any?, xcmVersion: XcmVersion): InitiateTeleport {
                val struct = value.castToStruct()

                return InitiateTeleport(
                    assets = MultiAssetFilter.bind(struct["assets"], xcmVersion),
                    dest = RelativeMultiLocation.bind(struct["dest"]),
                    xcm = XcmMessage.bindKnown(struct["xcm"], xcmVersion)
                )
            }
        }

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
            return DictEnum.Entry(
                name = "InitiateTeleport",
                value = structOf(
                    "assets" to assets.toEncodableInstance(xcmVersion),
                    "dest" to dest.toEncodableInstance(xcmVersion),
                    "xcm" to xcm.toEncodableInstance(xcmVersion)
                )
            )
        }
    }
}

typealias VersionedXcmMessage = VersionedXcm<XcmMessage>
