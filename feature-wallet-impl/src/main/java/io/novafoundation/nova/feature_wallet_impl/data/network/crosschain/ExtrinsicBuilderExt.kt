package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.XcmMultiAsset.Fungibility
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder

fun ExtrinsicBuilder.xcmExecute(
    message: VersionedXcm,
    maxWeight: Weight,
): ExtrinsicBuilder {
    return call(
        moduleName = runtime.metadata.xcmPalletName(),
        callName = "execute",
        arguments = mapOf(
            "message" to message.toEncodableInstance(),
            "max_weight" to maxWeight
        )
    )
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
            "weight_limit" to weightLimit.toEncodableInstance()
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

fun WeightLimit.toEncodableInstance() = when (this) {
    is WeightLimit.Limited -> DictEnum.Entry("Limited", weight)
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

fun MultiLocation.toEncodableInstance() = structOf(
    "parents" to parents,
    "interior" to interior.toEncodableInstance()
)

fun VersionedMultiAssets.toEncodableInstance() = when (this) {
    is VersionedMultiAssets.V1 -> DictEnum.Entry(
        name = "V1",
        value = assets.toEncodableInstance()
    )
}

fun VersionedMultiAsset.toEncodableInstance() = when (this) {
    is VersionedMultiAsset.V1 -> DictEnum.Entry(
        name = "V1",
        value = asset.toEncodableInstance()
    )
}

fun VersionedMultiLocation.toEncodableInstance() = when (this) {
    is VersionedMultiLocation.V1 -> DictEnum.Entry(
        name = "V1",
        value = multiLocation.toEncodableInstance()
    )
}

private fun MultiLocation.Interior.toEncodableInstance() = when (this) {
    MultiLocation.Interior.Here -> DictEnum.Entry("Here", null)

    is MultiLocation.Interior.Junctions -> if (junctions.size == 1) {
        DictEnum.Entry(
            name = "X1",
            value = junctions.first().toEncodableInstance()
        )
    } else {
        DictEnum.Entry(
            name = "X${junctions.size}",
            value = junctions.map(MultiLocation.Junction::toEncodableInstance)
        )
    }
}

private fun MultiLocation.Junction.toEncodableInstance() = when (this) {
    is MultiLocation.Junction.GeneralKey -> DictEnum.Entry("GeneralKey", key.fromHex())
    is MultiLocation.Junction.PalletInstance -> DictEnum.Entry("PalletInstance", index)
    is MultiLocation.Junction.ParachainId -> DictEnum.Entry("Parachain", id)
    is MultiLocation.Junction.AccountKey20 -> DictEnum.Entry("AccountKey20", accountId.toJunctionAccountIdInstance(accountIdKey = "key"))
    is MultiLocation.Junction.AccountId32 -> DictEnum.Entry("AccountId32", accountId.toJunctionAccountIdInstance(accountIdKey = "id"))
    is MultiLocation.Junction.GeneralIndex -> DictEnum.Entry("GeneralIndex", index)
}

private fun AccountId.toJunctionAccountIdInstance(accountIdKey: String) = structOf(
    "network" to DictEnum.Entry("Any", null),
    accountIdKey to this
)
