package io.novafoundation.nova.feature_assets.presentation.balance.common.holders

import android.view.View
import coil.ImageLoader
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableChildViewHolder
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableChildItem
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadTokenIcon
import io.novafoundation.nova.feature_assets.presentation.balance.common.BalanceListAdapter
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenAssetUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import kotlinx.android.synthetic.main.item_token_asset.view.itemTokenAssetImage
import kotlinx.android.synthetic.main.item_token_asset.view.itemTokenAssetToken
import kotlinx.android.synthetic.main.item_token_asset.view.itemTokenAssetBalance
import kotlinx.android.synthetic.main.item_token_asset.view.itemTokenAssetChainIcon
import kotlinx.android.synthetic.main.item_token_asset.view.itemTokenAssetChainName
import kotlinx.android.synthetic.main.item_token_asset.view.itemTokenAssetPriceAmount

class TokenAssetViewHolder(
    containerView: View,
    private val imageLoader: ImageLoader,
) : GroupedListHolder(containerView), ExpandableChildViewHolder {

    override var expandableItem: ExpandableChildItem? = null

    fun bind(tokenAsset: TokenAssetUi, itemHandler: BalanceListAdapter.ItemAssetHandler) = with(containerView) {
        expandableItem = tokenAsset

        val asset = tokenAsset.asset
        itemTokenAssetImage.setIcon(tokenAsset.assetIcon, imageLoader)
        itemTokenAssetChainIcon.loadTokenIcon(tokenAsset.chain.icon, imageLoader)
        itemTokenAssetChainName.text = tokenAsset.chain.name

        bindTotal(asset)

        itemTokenAssetToken.text = asset.token.configuration.symbol.value

        setOnClickListener { itemHandler.assetClicked(asset) }
    }

    fun bindTotal(asset: AssetModel) {
        containerView.itemTokenAssetBalance.text = asset.amount.token
        containerView.itemTokenAssetPriceAmount.text = asset.amount.fiat
    }
}
