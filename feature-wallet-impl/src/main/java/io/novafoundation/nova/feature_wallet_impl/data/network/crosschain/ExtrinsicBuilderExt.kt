package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.common.utils.argument
import io.novafoundation.nova.common.utils.requireActualType
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.toEncodableInstance
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.XcmMultiAsset.Fungibility
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.Type
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.primitives.NumberType
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import jp.co.soramitsu.fearless_utils.runtime.metadata.call
import jp.co.soramitsu.fearless_utils.runtime.metadata.module

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

private fun VersionedXcm.toEncodableInstance() = when (this) {
    is VersionedXcm.V2 -> DictEnum.Entry(
        name = "V2",
        value = message.toEncodableInstance()
    )
}

private fun XcmV2.toEncodableInstance(): Any = instructions.map(XcmV2Instruction::toEncodableInstance)

private fun XcmMultiAssets.toEncodableInstance() = map(XcmMultiAsset::toEncodableInstance)
private fun XcmMultiAsset.toEncodableInstance() = structOf(
    "fun" to fungibility.toEncodableInstance(),
    "id" to id.toEncodableInstance()
)

private fun XcmMultiAsset.Id.toEncodableInstance() = when (this) {
    is XcmMultiAsset.Id.Concrete -> DictEnum.Entry(
        name = "Concrete",
        value = multiLocation.toEncodableInstance()
    )
}

private fun Fungibility.toEncodableInstance() = when (this) {
    is Fungibility.Fungible -> DictEnum.Entry(
        name = "Fungible",
        value = amount
    )
}

private fun XcmV2Instruction.toEncodableInstance() = when (this) {
    is XcmV2Instruction.WithdrawAsset -> DictEnum.Entry(
        name = "WithdrawAsset",
        value = assets.toEncodableInstance()
    )
    is XcmV2Instruction.DepositAsset -> DictEnum.Entry(
        name = "DepositAsset",
        value = structOf(
            "assets" to assets.toEncodableInstance(),
            "max_assets" to maxAssets,
            "beneficiary" to beneficiary.toEncodableInstance()
        )
    )
    is XcmV2Instruction.BuyExecution -> DictEnum.Entry(
        name = "BuyExecution",
        value = structOf(
            "fees" to fees.toEncodableInstance(),
            // xcm v2 always uses v1 weights
            "weight_limit" to weightLimit.toV1EncodableInstance()
        )
    )
    XcmV2Instruction.ClearOrigin -> DictEnum.Entry(
        name = "ClearOrigin",
        value = null
    )
    is XcmV2Instruction.ReserveAssetDeposited -> DictEnum.Entry(
        name = "ReserveAssetDeposited",
        value = assets.toEncodableInstance()
    )
    is XcmV2Instruction.DepositReserveAsset -> DictEnum.Entry(
        name = "DepositReserveAsset",
        value = structOf(
            "assets" to assets.toEncodableInstance(),
            "max_assets" to maxAssets,
            "dest" to dest.toEncodableInstance(),
            "xcm" to xcm.toEncodableInstance()
        )
    )
    is XcmV2Instruction.ReceiveTeleportedAsset -> DictEnum.Entry(
        name = "ReceiveTeleportedAsset",
        value = assets.toEncodableInstance()
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
        value = assets.toEncodableInstance()
    )
    is VersionedMultiAssets.V2 -> DictEnum.Entry(
        name = "V2",
        value = assets.toEncodableInstance()
    )
}

fun VersionedMultiAsset.toEncodableInstance() = when (this) {
    is VersionedMultiAsset.V1 -> DictEnum.Entry(
        name = "V1",
        value = asset.toEncodableInstance()
    )
    is VersionedMultiAsset.V2 -> DictEnum.Entry(
        name = "V2",
        value = asset.toEncodableInstance()
    )
}

fun VersionedMultiLocation.toEncodableInstance() = when (this) {
    is VersionedMultiLocation.V1 -> DictEnum.Entry(
        name = "V1",
        value = multiLocation.toEncodableInstance()
    )
    is VersionedMultiLocation.V2 -> DictEnum.Entry(
        name = "V2",
        value = multiLocation.toEncodableInstance()
    )
}
