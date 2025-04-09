package io.novafoundation.nova.feature_xcm_api.builder

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.feature_xcm_api.asset.MultiAsset
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetFilter
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetFilter.Wild.All
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetId
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssets
import io.novafoundation.nova.feature_xcm_api.asset.intoMultiAssets
import io.novafoundation.nova.feature_xcm_api.asset.withAmount
import io.novafoundation.nova.feature_xcm_api.builder.fees.MeasureXcmFees
import io.novafoundation.nova.feature_xcm_api.builder.fees.PayFeesMode
import io.novafoundation.nova.feature_xcm_api.message.VersionedXcmMessage
import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.weight.WeightLimit

interface XcmBuilder : XcmContext {

    interface Factory {

        fun create(
            initial: AbsoluteMultiLocation,
            xcmVersion: XcmVersion,
            measureXcmFees: MeasureXcmFees
        ): XcmBuilder
    }

    fun payFees(payFeesMode: PayFeesMode)

    fun withdrawAsset(assets: MultiAssets)

    fun buyExecution(fees: MultiAsset, weightLimit: WeightLimit)

    // We only support depositing to a accountId. We might extend it in the future with no issues
    // but we keep the support limited to simplify implementation
    fun depositAsset(assets: MultiAssetFilter, beneficiary: AccountIdKey)

    // Performs context change
    fun transferReserveAsset(assets: MultiAssets, dest: AbsoluteMultiLocation)

    // Performs context change
    fun initiateReserveWithdraw(assets: MultiAssetFilter, reserve: AbsoluteMultiLocation)

    // Performs context change
    fun depositReserveAsset(assets: MultiAssetFilter, dest: AbsoluteMultiLocation)

    suspend fun build(): VersionedXcmMessage
}

fun XcmBuilder.withdrawAsset(asset: AbsoluteMultiLocation, amount: BalanceOf) {
    withdrawAsset(MultiAsset.from(asset.relativeToLocal(), amount).intoMultiAssets())
}

fun XcmBuilder.transferReserveAsset(asset: AbsoluteMultiLocation, amount: BalanceOf, dest: AbsoluteMultiLocation) {
    transferReserveAsset(MultiAsset.from(asset.relativeToLocal(), amount).intoMultiAssets(), dest)
}

fun XcmBuilder.buyExecution(asset: AbsoluteMultiLocation, amount: BalanceOf, weightLimit: WeightLimit) {
    buyExecution(MultiAsset.from(asset.relativeToLocal(), amount), weightLimit)
}

fun XcmBuilder.depositAllAssetsTo(beneficiary: AccountIdKey) {
    depositAsset(All, beneficiary)
}

fun XcmBuilder.payFeesIn(assetId: MultiAssetId) {
    payFees(PayFeesMode.Measured(assetId))
}

fun XcmBuilder.payFeesIn(assetLocation: AbsoluteMultiLocation) {
    payFeesIn(MultiAssetId(assetLocation.relativeToLocal()))
}

fun XcmBuilder.payFees(assetId: MultiAssetId, exactFees: BalanceOf) {
    payFees(PayFeesMode.Exact(assetId.withAmount(exactFees)))
}
