package io.novafoundation.nova.feature_xcm_api.message

import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.feature_xcm_api.asset.MultiAsset
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetFilter
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssets
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.versions.VersionedToDynamicScaleInstance
import io.novafoundation.nova.feature_xcm_api.versions.VersionedXcm
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.versions.versionedXcm
import io.novafoundation.nova.feature_xcm_api.weight.WeightLimit
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import java.math.BigInteger

@JvmInline
value class XcmMessage(val instructions: List<XcmInstruction>) : VersionedToDynamicScaleInstance {

    constructor(vararg instructions: XcmInstruction) : this(instructions.toList())

    override fun toString(): String {
        return instructions.toString()
    }

    override fun toEncodableInstance(xcmVersion: XcmVersion): Any? {
        return instructions.map { it.toEncodableInstance(xcmVersion) }
    }
}

fun List<XcmInstruction>.asXcmMessage(): XcmMessage = XcmMessage(this)

fun List<XcmInstruction>.asVersionedXcmMessage(version: XcmVersion): VersionedXcmMessage = asXcmMessage().versionedXcm(version)

sealed class XcmInstruction : VersionedToDynamicScaleInstance {

    data class WithdrawAsset(val assets: MultiAssets) : XcmInstruction() {

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

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any? {
            return DictEnum.Entry(
                name = "DepositAsset",
                value = structOf(
                    "assets" to assets.toEncodableInstance(),
                    "beneficiary" to beneficiary.toEncodableInstance(xcmVersion),
                    // Used in XCM V2 and below. We put 1 here since we only support cases for transferring a single asset
                    "max_assets" to BigInteger.ONE,
                )
            )
        }
    }

    data class BuyExecution(val fees: MultiAsset, val weightLimit: WeightLimit) : XcmInstruction() {

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

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
            return DictEnum.Entry(
                name = "ReserveAssetDeposited",
                value = assets.toEncodableInstance(xcmVersion)
            )
        }
    }

    data class ReceiveTeleportedAsset(val assets: MultiAssets) : XcmInstruction() {
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

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
            return DictEnum.Entry(
                name = "InitiateReserveWithdraw",
                value = structOf(
                    "assets" to assets.toEncodableInstance(),
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

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
            return DictEnum.Entry(
                name = "DepositReserveAsset",
                value = structOf(
                    "assets" to assets.toEncodableInstance(),
                    // Used in XCM V2 and below. We put 1 here since we only support cases for transferring a single asset
                    "max_assets" to BigInteger.ONE,
                    "dest" to dest.toEncodableInstance(xcmVersion),
                    "xcm" to xcm.toEncodableInstance(xcmVersion)
                )
            )
        }
    }

    data class PayFees(val fees: MultiAsset) : XcmInstruction() {
        override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
            return DictEnum.Entry(
                name = "PayFees",
                value = structOf(
                    "asset" to fees.toEncodableInstance(xcmVersion)
                )
            )
        }
    }

    data class InitiateTeleport(
        val assets: MultiAssetFilter,
        val dest: RelativeMultiLocation,
        val xcm: XcmMessage
    ) : XcmInstruction() {

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
            return DictEnum.Entry(
                name = "InitiateTeleport",
                value = structOf(
                    "assets" to assets.toEncodableInstance(),
                    "dest" to dest.toEncodableInstance(xcmVersion),
                    "xcm" to xcm.toEncodableInstance(xcmVersion)
                )
            )
        }
    }

    data class ExchangeAsset(
        val give: MultiAssetFilter,
        val want: MultiAssets,
        val maximal: Boolean
    ) : XcmInstruction() {

        override fun toEncodableInstance(xcmVersion: XcmVersion): Any? {
            return DictEnum.Entry(
                name = "ExchangeAsset",
                value = structOf(
                    "give" to give.toEncodableInstance(),
                    "want" to want.toEncodableInstance(xcmVersion),
                    "maximal" to maximal
                )
            )
        }
    }
}

typealias VersionedXcmMessage = VersionedXcm<XcmMessage>

inline fun <reified T : XcmInstruction> VersionedXcmMessage.modifyInstruction(transform: (T) -> XcmInstruction): VersionedXcmMessage {
    val newInstructions = xcm.instructions.map {
        if (it is T) {
            transform(it)
        } else {
            it
        }
    }

    return copy(xcm = newInstructions.asXcmMessage())
}

inline fun <reified T : XcmInstruction> VersionedXcmMessage.modifyInstructionOrPrepend(lazyInstruction: (old: T?) -> XcmInstruction): VersionedXcmMessage {
    var found = false

    val newInstructions = xcm.instructions.mapTo(mutableListOf()) {
        if (it is T) {
            found = true

            lazyInstruction(it)
        } else {
            it
        }
    }

    if (!found) {
        newInstructions.add(0, lazyInstruction(null))
    }

    return copy(xcm = newInstructions.asXcmMessage())
}

inline fun <reified T : XcmInstruction> VersionedXcmMessage.findInstruction(): T? {
    return xcm.instructions.find { it is T } as T
}

