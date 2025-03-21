package io.novafoundation.nova.feature_swap_impl.data.assetExchange.xcm

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetIdWithAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.feature_xcm_api.asset.MultiAsset
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetFilter
import io.novafoundation.nova.feature_xcm_api.asset.MultiAssets
import io.novafoundation.nova.feature_xcm_api.asset.intoMultiAssets
import io.novafoundation.nova.feature_xcm_api.builder.XcmBuilder
import io.novafoundation.nova.feature_xcm_api.builder.XcmContext
import io.novafoundation.nova.feature_xcm_api.builder.relativeToLocal
import io.novafoundation.nova.feature_xcm_api.message.VersionedXcmMessage
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation
import io.novafoundation.nova.feature_xcm_api.weight.WeightLimit
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

interface SwapXcmBuilder {

    suspend fun withdrawAsset(vararg asset: ChainAssetIdWithAmount)

    suspend fun buyExecution(asset: ChainAssetIdWithAmount, weightLimit: WeightLimit)

    // We only support depositing to a accountId. We might extend it in the future with no issues
    // but we keep the support limited to simplify implementation
    suspend fun depositAsset(assets: MultiAssetFilter, beneficiary: AccountIdKey)

    // Performs context change
    suspend fun transferReserveAsset(vararg asset: ChainAssetIdWithAmount, dest: ChainId)

    // Performs context change
    suspend fun initiateReserveWithdraw(assets: MultiAssetFilter, reserve: ChainId)

    // Performs context change
    suspend fun depositReserveAsset(assets: MultiAssetFilter, dest: ChainId)

    // Performs context change
    suspend fun initiateTeleport(assets: MultiAssetFilter, dest: ChainId)

    suspend fun exchangeAsset(give: MultiAssetFilter, vararg want: ChainAssetIdWithAmount, maximal: Boolean)

    suspend fun build(): VersionedXcmMessage
}

suspend fun SwapXcmBuilder.exchangeAllAssets(swapLimit: SwapLimit, want: FullChainAssetId) {
    when(swapLimit) {
        is SwapLimit.SpecifiedIn -> exchangeAsset(MultiAssetFilter.Wild.All, want.withAmount(swapLimit.amountOutMin), maximal = true)
        is SwapLimit.SpecifiedOut -> exchangeAsset(MultiAssetFilter.Wild.All, want.withAmount(swapLimit.amountOut), maximal = false)
    }
}

class ProxyingSwapXcmBuilder(
    private val xcmBuilder: XcmBuilder,
    private val chainLocationConverter: XcmLocationConverter,
): SwapXcmBuilder, XcmContext by xcmBuilder {

    override suspend fun withdrawAsset(vararg asset: ChainAssetIdWithAmount) {
        xcmBuilder.withdrawAsset(asset.toMultiAssets())
    }

    override suspend fun buyExecution(asset: ChainAssetIdWithAmount, weightLimit: WeightLimit) {
        xcmBuilder.buyExecution(asset.toMultiAsset(), weightLimit)
    }

    override suspend fun depositAsset(assets: MultiAssetFilter, beneficiary: AccountIdKey) {
        xcmBuilder.depositAsset(assets, beneficiary)
    }

    override suspend fun transferReserveAsset(vararg asset: ChainAssetIdWithAmount, dest: ChainId) {
       xcmBuilder.transferReserveAsset(asset.toMultiAssets(), dest.toChainLocation())
    }

    override suspend fun initiateReserveWithdraw(assets: MultiAssetFilter, reserve: ChainId) {
       xcmBuilder.initiateReserveWithdraw(assets, reserve.toChainLocation())
    }

    override suspend fun depositReserveAsset(assets: MultiAssetFilter, dest: ChainId) {
        xcmBuilder.depositReserveAsset(assets, dest.toChainLocation())
    }

    override suspend fun initiateTeleport(assets: MultiAssetFilter, dest: ChainId) {
        xcmBuilder.initiateTeleport(assets, dest.toChainLocation())
    }

    override suspend fun exchangeAsset(give: MultiAssetFilter, vararg want: ChainAssetIdWithAmount, maximal: Boolean) {
        xcmBuilder.exchangeAsset(give, want.toMultiAssets(), maximal)
    }

    override suspend fun build(): VersionedXcmMessage {
       return xcmBuilder.build()
    }

    private suspend fun ChainId.toChainLocation(): ChainLocation {
        return chainLocationConverter.getChainLocation(this)
    }

    private suspend fun Array<out ChainAssetIdWithAmount>.toMultiAssets() : MultiAssets {
        return map { it.toMultiAsset() }.intoMultiAssets()
    }

    private suspend fun ChainAssetIdWithAmount.toMultiAsset(): MultiAsset {
        val assetLocation = chainLocationConverter.getAssetLocation(chainAssetId)
        return MultiAsset.from(assetLocation.location.relativeToLocal(), amount)
    }
}
