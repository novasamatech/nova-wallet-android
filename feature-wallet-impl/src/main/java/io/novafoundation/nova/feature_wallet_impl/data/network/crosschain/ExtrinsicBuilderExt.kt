package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.common.utils.argument
import io.novafoundation.nova.common.utils.requireActualType
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.XcmMultiAsset.Fungibility
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.XcmVersion
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.toEncodableInstance
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.Type
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.NumberType
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.call
import io.novasama.substrate_sdk_android.runtime.metadata.module

fun ExtrinsicBuilder.xcmExecute(
    message: VersionedXcm,
    maxWeight: Weight,
): ExtrinsicBuilder {
    return call(
        moduleName = runtime.metadata.xcmPalletName(),
        callName = "execute",
        arguments = mapOf(
            "message" to message.toEncodableInstance(),
            "max_weight" to runtime.prepareWeightForEncoding(maxWeight)
        )
    )
}

private fun RuntimeSnapshot.prepareWeightForEncoding(weight: Weight): Any {
    val moduleName = metadata.xcmPalletName()

    val weightArgumentType = metadata.module(moduleName)
        .call("execute")
        .argument("max_weight")
        .requireActualType()

    return when {
        weightArgumentType.isWeightV1() -> weight
        else -> weight.encodeWeightV2()
    }
}

private fun Weight.encodeWeightV2(): Struct.Instance {
    return structOf("refTime" to this, "proofSize" to Balance.ZERO)
}

private fun Type<*>.isWeightV1(): Boolean {
    return this is NumberType
}

fun VersionedXcm.toEncodableInstance() = when (this) {
    is VersionedXcm.V2 -> DictEnum.Entry(
        name = "V2",
        value = message.toEncodableInstance(XcmVersion.V2)
    )

    is VersionedXcm.V3 -> DictEnum.Entry(
        name = "V3",
        value = message.toEncodableInstance(XcmVersion.V3)
    )
}

private fun XcmMessage.toEncodableInstance(xcmVersion: XcmVersion): Any = instructions.map { it.toEncodableInstance(xcmVersion) }

private fun XcmMultiAssets.toEncodableInstance(xcmVersion: XcmVersion) = map { it.toEncodableInstance(xcmVersion) }
private fun XcmMultiAsset.toEncodableInstance(xcmVersion: XcmVersion) = structOf(
    "fun" to fungibility.toEncodableInstance(),
    "id" to id.toEncodableInstance(xcmVersion)
)

private fun XcmMultiAsset.Id.toEncodableInstance(xcmVersion: XcmVersion) = when (this) {
    is XcmMultiAsset.Id.Concrete -> DictEnum.Entry(
        name = "Concrete",
        value = multiLocation.toEncodableInstance(xcmVersion)
    )
}

private fun Fungibility.toEncodableInstance() = when (this) {
    is Fungibility.Fungible -> DictEnum.Entry(
        name = "Fungible",
        value = amount
    )
}

private fun XcmVInstruction.toEncodableInstance(xcmVersion: XcmVersion) = when (this) {
    is XcmVInstruction.WithdrawAsset -> DictEnum.Entry(
        name = "WithdrawAsset",
        value = assets.toEncodableInstance(xcmVersion)
    )

    is XcmVInstruction.DepositAsset -> DictEnum.Entry(
        name = "DepositAsset",
        value = structOf(
            "assets" to assets.toEncodableInstance(),
            "max_assets" to maxAssets,
            "beneficiary" to beneficiary.toEncodableInstance(xcmVersion)
        )
    )

    is XcmVInstruction.BuyExecution -> DictEnum.Entry(
        name = "BuyExecution",
        value = structOf(
            "fees" to fees.toEncodableInstance(xcmVersion),
            // xcm v2 always uses v1 weights
            "weight_limit" to weightLimit.toV1EncodableInstance()
        )
    )

    XcmVInstruction.ClearOrigin -> DictEnum.Entry(
        name = "ClearOrigin",
        value = null
    )

    is XcmVInstruction.ReserveAssetDeposited -> DictEnum.Entry(
        name = "ReserveAssetDeposited",
        value = assets.toEncodableInstance(xcmVersion)
    )

    is XcmVInstruction.DepositReserveAsset -> DictEnum.Entry(
        name = "DepositReserveAsset",
        value = structOf(
            "assets" to assets.toEncodableInstance(),
            "max_assets" to maxAssets,
            "dest" to dest.toEncodableInstance(xcmVersion),
            "xcm" to xcm.toEncodableInstance(xcmVersion)
        )
    )

    is XcmVInstruction.ReceiveTeleportedAsset -> DictEnum.Entry(
        name = "ReceiveTeleportedAsset",
        value = assets.toEncodableInstance(xcmVersion)
    )
}

fun WeightLimit.toV1EncodableInstance() = when (this) {
    is WeightLimit.Limited -> DictEnum.Entry("Limited", weight)
    WeightLimit.Unlimited -> DictEnum.Entry("Unlimited", null)
}

fun WeightLimit.toVersionedEncodableInstance(runtimeSnapshot: RuntimeSnapshot) = when (this) {
    is WeightLimit.Limited -> DictEnum.Entry("Limited", runtimeSnapshot.prepareWeightForEncoding(weight))
    WeightLimit.Unlimited -> DictEnum.Entry("Unlimited", null)
}

private fun XcmMultiAssetFilter.toEncodableInstance() = when (this) {
    XcmMultiAssetFilter.Wild.All -> DictEnum.Entry(
        name = "Wild",
        value = DictEnum.Entry(
            name = "All",
            value = null
        )
    )
}

fun VersionedMultiAssets.toEncodableInstance() = when (this) {
    is VersionedMultiAssets.V1 -> DictEnum.Entry(
        name = "V1",
        value = assets.toEncodableInstance(XcmVersion.V1)
    )

    is VersionedMultiAssets.V2 -> DictEnum.Entry(
        name = "V2",
        value = assets.toEncodableInstance(XcmVersion.V2)
    )

    is VersionedMultiAssets.V3 -> DictEnum.Entry(
        name = "V3",
        value = assets.toEncodableInstance(XcmVersion.V3)
    )
}

fun VersionedMultiAsset.toEncodableInstance() = when (this) {
    is VersionedMultiAsset.V1 -> DictEnum.Entry(
        name = "V1",
        value = asset.toEncodableInstance(XcmVersion.V1)
    )

    is VersionedMultiAsset.V2 -> DictEnum.Entry(
        name = "V2",
        value = asset.toEncodableInstance(XcmVersion.V2)
    )

    is VersionedMultiAsset.V3 -> DictEnum.Entry(
        name = "V3",
        value = asset.toEncodableInstance(XcmVersion.V3)
    )
}

fun VersionedMultiLocation.toEncodableInstance() = when (this) {
    is VersionedMultiLocation.V1 -> DictEnum.Entry(
        name = "V1",
        value = multiLocation.toEncodableInstance(XcmVersion.V1)
    )

    is VersionedMultiLocation.V2 -> DictEnum.Entry(
        name = "V2",
        value = multiLocation.toEncodableInstance(XcmVersion.V2)
    )

    is VersionedMultiLocation.V3 -> DictEnum.Entry(
        name = "V3",
        value = multiLocation.toEncodableInstance(XcmVersion.V3)
    )
}
