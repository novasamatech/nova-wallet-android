package io.novafoundation.nova.feature_assets.presentation.balance.common.holders

import android.view.View
import coil.ImageLoader
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableViewHolder
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadTokenIcon
import io.novafoundation.nova.feature_assets.presentation.balance.common.BalanceListAdapter
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import kotlinx.android.synthetic.main.item_token_asset_group.view.itemAssetTokenGroupBalance
import kotlinx.android.synthetic.main.item_token_asset_group.view.itemAssetTokenGroupPriceAmount
import kotlinx.android.synthetic.main.item_token_asset_group.view.itemAssetTokenGroupRate
import kotlinx.android.synthetic.main.item_token_asset_group.view.itemAssetTokenGroupRateChange
import kotlinx.android.synthetic.main.item_token_asset_group.view.itemAssetTokenGroupToken
import kotlinx.android.synthetic.main.item_token_asset_group.view.itemTokenGroupAssetImage


class TokenAssetGroupViewHolder(
    containerView: View,
    private val imageLoader: ImageLoader,
) : GroupedListHolder(containerView), ExpandableViewHolder {

    fun bind(tokenGroup: TokenGroupUi, itemHandler: BalanceListAdapter.ItemAssetHandler) = with(containerView) {
        itemTokenGroupAssetImage.loadTokenIcon(tokenGroup.tokenIcon, imageLoader)

        bindPriceRate(tokenGroup)

        bindRecentChange(tokenGroup)

        bindTotal(tokenGroup)

        itemAssetTokenGroupToken.text = tokenGroup.tokenSymbol

        setOnClickListener { itemHandler.tokenGroupClicked(tokenGroup) }
    }

    fun bindTotal(networkAsset: TokenGroupUi) {
        val balance = networkAsset.balance
        containerView.itemAssetTokenGroupBalance.text = balance.token
        containerView.itemAssetTokenGroupPriceAmount.text = balance.fiat
    }

    fun bindRecentChange(networkAsset: TokenGroupUi) = with(containerView) {
        itemAssetTokenGroupRateChange.setTextColorRes(networkAsset.rateChangeColorRes)
        itemAssetTokenGroupRateChange.text = networkAsset.recentRateChange
    }

    fun bindPriceRate(networkAsset: TokenGroupUi) = with(containerView) {
        itemAssetTokenGroupRate.text = networkAsset.rate
    }
}
