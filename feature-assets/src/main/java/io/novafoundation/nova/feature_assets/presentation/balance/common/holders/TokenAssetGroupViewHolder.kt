package io.novafoundation.nova.feature_assets.presentation.balance.common.holders

import coil.ImageLoader
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.presentation.masking.setMaskableText
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableParentViewHolder
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableParentItem
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_account_api.presenatation.chain.setTokenIcon
import io.novafoundation.nova.feature_assets.databinding.ItemTokenAssetGroupBinding
import io.novafoundation.nova.feature_assets.presentation.balance.common.BalanceListAdapter
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_wallet_api.presentation.model.maskableFiat
import io.novafoundation.nova.feature_wallet_api.presentation.model.maskableToken

class TokenAssetGroupViewHolder(
    private val binder: ItemTokenAssetGroupBinding,
    private val imageLoader: ImageLoader,
    private val itemHandler: BalanceListAdapter.ItemAssetHandler,
) : GroupedListHolder(binder.root), ExpandableParentViewHolder {

    override var expandableItem: ExpandableParentItem? = null

    fun bind(tokenGroup: TokenGroupUi) = with(binder) {
        updateExpandableItem(tokenGroup)

        itemTokenGroupAssetImage.setTokenIcon(tokenGroup.tokenIcon, imageLoader)

        bindPriceRateInternal(tokenGroup)

        bindRecentChangeInternal(tokenGroup)

        bindTotalInternal(tokenGroup)

        updateListener(tokenGroup)

        itemAssetTokenGroupToken.text = tokenGroup.tokenSymbol
    }

    fun bindTotal(networkAsset: TokenGroupUi) {
        updateListener(networkAsset)
        bindTotalInternal(networkAsset)
    }

    fun bindRecentChange(networkAsset: TokenGroupUi) {
        updateListener(networkAsset)
        bindRecentChangeInternal(networkAsset)
    }

    fun bindPriceRate(networkAsset: TokenGroupUi) {
        updateListener(networkAsset)
        bindPriceRateInternal(networkAsset)
    }

    fun bindGroupType(networkAsset: TokenGroupUi) {
        updateListener(networkAsset)
    }

    private fun bindTotalInternal(networkAsset: TokenGroupUi) {
        val balance = networkAsset.balance
        binder.itemAssetTokenGroupBalance.setMaskableText(balance.maskableToken())
        binder.itemAssetTokenGroupPriceAmount.setMaskableText(balance.maskableFiat())
    }

    private fun bindRecentChangeInternal(networkAsset: TokenGroupUi) {
        with(binder) {
            itemAssetTokenGroupRateChange.setTextColorRes(networkAsset.rateChangeColorRes)
            itemAssetTokenGroupRateChange.text = networkAsset.recentRateChange
        }
    }

    private fun bindPriceRateInternal(networkAsset: TokenGroupUi) {
        with(binder) {
            itemAssetTokenGroupRate.text = networkAsset.rate
        }
    }

    private fun updateListener(tokenGroupUi: TokenGroupUi) {
        containerView.setOnClickListener { itemHandler.tokenGroupClicked(tokenGroupUi) }
    }
}
