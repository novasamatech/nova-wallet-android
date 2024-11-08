package io.novafoundation.nova.feature_assets.presentation.balance.common.holders

import android.view.View
import coil.ImageLoader
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_account_api.presenatation.chain.setTokenIcon
import io.novafoundation.nova.feature_assets.presentation.balance.common.BalanceListAdapter
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.NetworkAssetUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import kotlinx.android.synthetic.main.item_network_asset.view.itemAssetBalance
import kotlinx.android.synthetic.main.item_network_asset.view.itemAssetImage
import kotlinx.android.synthetic.main.item_network_asset.view.itemAssetPriceAmount
import kotlinx.android.synthetic.main.item_network_asset.view.itemAssetRate
import kotlinx.android.synthetic.main.item_network_asset.view.itemAssetRateChange
import kotlinx.android.synthetic.main.item_network_asset.view.itemAssetToken

class NetworkAssetViewHolder(
    containerView: View,
    private val imageLoader: ImageLoader,
) : GroupedListHolder(containerView) {

    fun bind(networkAsset: NetworkAssetUi, itemHandler: BalanceListAdapter.ItemAssetHandler) = with(containerView) {
        val asset = networkAsset.asset
        itemAssetImage.setTokenIcon(networkAsset.icon, imageLoader)

        bindPriceInfo(asset)

        bindRecentChange(asset)

        bindTotal(asset)

        itemAssetToken.text = asset.token.configuration.symbol.value

        setOnClickListener { itemHandler.assetClicked(asset) }
    }

    fun bindTotal(asset: AssetModel) {
        containerView.itemAssetBalance.text = asset.amount.token
        containerView.itemAssetPriceAmount.text = asset.amount.fiat
    }

    fun bindRecentChange(asset: AssetModel) = with(containerView) {
        itemAssetRateChange.setTextColorRes(asset.token.rateChangeColorRes)
        itemAssetRateChange.text = asset.token.recentRateChange
    }

    fun bindPriceInfo(asset: AssetModel) = with(containerView) {
        itemAssetRate.text = asset.token.rate
    }
}
