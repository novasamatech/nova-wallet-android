package io.novafoundation.nova.feature_assets.presentation.balance.common.holders

import android.view.View
import coil.ImageLoader
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadTokenIcon
import io.novafoundation.nova.feature_assets.presentation.balance.common.BalanceListAdapter
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.NetworkAssetUi
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
        itemAssetImage.loadTokenIcon(asset.token.configuration.iconUrl, imageLoader)

        bindPriceInfo(networkAsset)

        bindRecentChange(networkAsset)

        bindTotal(networkAsset)

        itemAssetToken.text = asset.token.configuration.symbol.value

        setOnClickListener { itemHandler.assetClicked(asset) }
    }

    fun bindTotal(networkAsset: NetworkAssetUi) {
        val asset = networkAsset.asset
        containerView.itemAssetBalance.text = asset.amount.token
        containerView.itemAssetPriceAmount.text = asset.amount.fiat
    }

    fun bindRecentChange(networkAsset: NetworkAssetUi) = with(containerView) {
        val asset = networkAsset.asset
        itemAssetRateChange.setTextColorRes(asset.token.rateChangeColorRes)
        itemAssetRateChange.text = asset.token.recentRateChange
    }

    fun bindPriceInfo(networkAsset: NetworkAssetUi) = with(containerView) {
        val asset = networkAsset.asset
        itemAssetRate.text = asset.token.rate
    }
}
