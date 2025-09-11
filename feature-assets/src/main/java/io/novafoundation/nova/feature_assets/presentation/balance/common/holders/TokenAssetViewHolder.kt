package io.novafoundation.nova.feature_assets.presentation.balance.common.holders

import coil.ImageLoader
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.presentation.masking.setMaskableText
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableChildViewHolder
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableChildItem
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_account_api.presenatation.chain.setTokenIcon
import io.novafoundation.nova.feature_assets.databinding.ItemTokenAssetBinding
import io.novafoundation.nova.feature_assets.presentation.balance.common.BalanceListAdapter
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenAssetUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.maskableFiat
import io.novafoundation.nova.feature_wallet_api.presentation.model.maskableToken

class TokenAssetViewHolder(
    private val binder: ItemTokenAssetBinding,
    private val imageLoader: ImageLoader,
) : GroupedListHolder(binder.root), ExpandableChildViewHolder {

    override var expandableItem: ExpandableChildItem? = null

    fun bind(tokenAsset: TokenAssetUi, itemHandler: BalanceListAdapter.ItemAssetHandler) = with(containerView) {
        updateExpandableItem(tokenAsset)

        val asset = tokenAsset.asset
        binder.itemTokenAssetImage.setTokenIcon(tokenAsset.assetIcon, imageLoader)
        binder.itemTokenAssetChainIcon.loadChainIcon(tokenAsset.chain.icon, imageLoader)
        binder.itemTokenAssetChainName.text = tokenAsset.chain.name

        bindTotal(asset)

        binder.itemTokenAssetToken.text = asset.token.configuration.symbol.value

        setOnClickListener { itemHandler.assetClicked(asset.token.configuration) }
    }

    fun bindTotal(asset: AssetModel) {
        binder.itemTokenAssetBalance.setMaskableText(asset.amount.maskableToken())
        binder.itemTokenAssetPriceAmount.setMaskableText(asset.amount.maskableFiat())
    }
}
