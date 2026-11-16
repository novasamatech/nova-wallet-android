package io.novafoundation.nova.feature_assets.presentation.swap.asset

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import coil.ImageLoader
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.feature_account_api.presenatation.chain.setTokenIcon
import io.novafoundation.nova.feature_assets.databinding.ItemSwapTokenBinding
import io.novafoundation.nova.feature_assets.databinding.ItemSwapTokenHeaderBinding

internal class SwapTokenSelectionAdapter(
    private val imageLoader: ImageLoader,
    private val onClick: (SwapTokenRow) -> Unit
) : GroupedListAdapter<SwapTokenHeader, SwapTokenRow>(SwapTokenDiff) {

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return HeaderHolder(ItemSwapTokenHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return TokenHolder(ItemSwapTokenBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun bindGroup(holder: GroupedListHolder, group: SwapTokenHeader) {
        (holder as HeaderHolder).binder.swapTokenSectionTitle.text = group.title
    }

    override fun bindChild(holder: GroupedListHolder, child: SwapTokenRow) {
        with((holder as TokenHolder).binder) {
            swapTokenIcon.setTokenIcon(child.icon, imageLoader)
            swapTokenNetworkBadge.setIcon(child.networkIcon, imageLoader)
            swapTokenName.text = child.title
            swapTokenNetwork.text = child.network
            swapTokenBalance.isVisible = child.amount != null
            swapTokenAmount.text = child.amount?.token
            swapTokenFiat.setTextOrHide(child.amount?.fiat)
            swapTokenSelectionBar.isVisible = child.selected
            root.isSelected = child.selected
            swapTokenIcon.isSelected = false
            root.setOnClickListener { onClick(child) }
        }
    }

    private class HeaderHolder(val binder: ItemSwapTokenHeaderBinding) : GroupedListHolder(binder.root)
    private class TokenHolder(val binder: ItemSwapTokenBinding) : GroupedListHolder(binder.root)
}

private object SwapTokenDiff : BaseGroupedDiffCallback<SwapTokenHeader, SwapTokenRow>(SwapTokenHeader::class.java) {
    override fun areGroupItemsTheSame(oldItem: SwapTokenHeader, newItem: SwapTokenHeader) = oldItem.title == newItem.title
    override fun areGroupContentsTheSame(oldItem: SwapTokenHeader, newItem: SwapTokenHeader) = oldItem == newItem
    override fun areChildItemsTheSame(oldItem: SwapTokenRow, newItem: SwapTokenRow) = oldItem.id == newItem.id
    override fun areChildContentsTheSame(oldItem: SwapTokenRow, newItem: SwapTokenRow) = oldItem == newItem
}
