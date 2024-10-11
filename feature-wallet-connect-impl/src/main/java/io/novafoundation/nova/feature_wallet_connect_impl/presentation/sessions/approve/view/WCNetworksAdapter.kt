package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve.view

import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.updateLayoutParams
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.view.ChainChipView
import io.novafoundation.nova.feature_wallet_connect_impl.R

class WCNetworksAdapter : GroupedListAdapter<WCNetworkListModel.Label, WCNetworkListModel.Chain>(WCNetworksDiffCallback()) {

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return WcNetworksLabelHolder(parent)
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return WcNetworksChainHolder(parent)
    }

    override fun bindChild(holder: GroupedListHolder, child: WCNetworkListModel.Chain) {
        (holder as WcNetworksChainHolder).bind(child.chainUi)
    }

    override fun bindGroup(holder: GroupedListHolder, group: WCNetworkListModel.Label) {
        (holder as WcNetworksLabelHolder).bind(group)
    }
}

private class WcNetworksChainHolder(rootView: ViewGroup) : GroupedListHolder(rootView.inflateChild(R.layout.item_bottom_sheet_chain_list)) {

    private val chainChipView = containerView as ChainChipView

    fun bind(item: ChainUi) {
        chainChipView.setChain(item)
    }
}

private class WcNetworksLabelHolder(rootView: ViewGroup) : GroupedListHolder(rootView.inflateChild(R.layout.item_bottom_sheet_wc_networks_label)) {

    fun bind(item: WCNetworkListModel.Label) = with(containerView.itemWcNetworksLabel) {
        updateLayoutParams<MarginLayoutParams> {
            if (item.needsAdditionalSeparator) {
                setMargins(16.dp(context), 12.dp(context), 16.dp(context), 4.dp(context))
            } else {
                setMargins(16.dp(context), 7.dp(context), 16.dp(context), 7.dp(context))
            }
        }

        text = item.name
    }
}

private class WCNetworksDiffCallback : BaseGroupedDiffCallback<WCNetworkListModel.Label, WCNetworkListModel.Chain>(WCNetworkListModel.Label::class.java) {

    override fun areGroupItemsTheSame(oldItem: WCNetworkListModel.Label, newItem: WCNetworkListModel.Label): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areGroupContentsTheSame(oldItem: WCNetworkListModel.Label, newItem: WCNetworkListModel.Label): Boolean {
        return true
    }

    override fun areChildItemsTheSame(oldItem: WCNetworkListModel.Chain, newItem: WCNetworkListModel.Chain): Boolean {
        return oldItem.chainUi.id == newItem.chainUi.id
    }

    override fun areChildContentsTheSame(oldItem: WCNetworkListModel.Chain, newItem: WCNetworkListModel.Chain): Boolean {
        return oldItem.chainUi == newItem.chainUi
    }
}
