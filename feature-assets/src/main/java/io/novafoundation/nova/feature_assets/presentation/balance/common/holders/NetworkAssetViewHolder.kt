package io.novafoundation.nova.feature_assets.presentation.balance.common.holders

import coil.ImageLoader
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.presentation.masking.setMaskableText
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_account_api.presenatation.chain.setTokenIcon
import io.novafoundation.nova.feature_assets.databinding.ItemNetworkAssetBinding
import io.novafoundation.nova.feature_assets.presentation.balance.common.BalanceListAdapter
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.NetworkAssetUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.maskableFiat
import io.novafoundation.nova.feature_wallet_api.presentation.model.maskableToken

class NetworkAssetViewHolder(
    private val binder: ItemNetworkAssetBinding,
    private val imageLoader: ImageLoader,
) : GroupedListHolder(binder.root) {

    fun bind(networkAsset: NetworkAssetUi, itemHandler: BalanceListAdapter.ItemAssetHandler) = with(containerView) {
        val asset = networkAsset.asset
        binder.itemAssetImage.setTokenIcon(networkAsset.icon, imageLoader)

        bindPriceInfo(asset)

        bindRecentChange(asset)

        bindTotal(asset)

        binder.itemAssetToken.text = asset.token.configuration.symbol.value

        setOnClickListener { itemHandler.assetClicked(asset.token.configuration) }
    }

    fun bindTotal(asset: AssetModel) {
        binder.itemAssetBalance.setMaskableText(asset.amount.maskableToken())
        binder.itemAssetPriceAmount.setMaskableText(asset.amount.maskableFiat())
    }

    fun bindRecentChange(asset: AssetModel) = with(containerView) {
        binder.itemAssetRateChange.setTextColorRes(asset.token.rateChangeColorRes)
        binder.itemAssetRateChange.text = asset.token.recentRateChange
    }

    fun bindPriceInfo(asset: AssetModel) = with(containerView) {
        binder.itemAssetRate.text = asset.token.rate
    }
}
