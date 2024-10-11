package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.list

import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.list.model.StakingProxyGroupRvItem
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.list.model.StakingProxyRvItem

class StakingProxyListAdapter(
    private val handler: Handler,
    private val imageLoader: ImageLoader
) : GroupedListAdapter<StakingProxyGroupRvItem, StakingProxyRvItem>(DiffCallback()) {

    interface Handler {
        fun onProxyClick(item: StakingProxyRvItem)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        val view = parent.inflateChild(R.layout.item_proxy_group)
        return StakingProxyGroupHolder(view)
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        val view = parent.inflateChild(R.layout.item_proxy)
        return StakingProxyHolder(handler, imageLoader, view)
    }

    override fun bindGroup(holder: GroupedListHolder, group: StakingProxyGroupRvItem) {
        require(holder is StakingProxyGroupHolder)
        holder.bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: StakingProxyRvItem) {
        require(holder is StakingProxyHolder)
        holder.bind(child)
    }
}

private class DiffCallback : BaseGroupedDiffCallback<StakingProxyGroupRvItem, StakingProxyRvItem>(StakingProxyGroupRvItem::class.java) {

    override fun areGroupItemsTheSame(oldItem: StakingProxyGroupRvItem, newItem: StakingProxyGroupRvItem): Boolean {
        return oldItem.text == newItem.text
    }

    override fun areGroupContentsTheSame(oldItem: StakingProxyGroupRvItem, newItem: StakingProxyGroupRvItem): Boolean {
        return true
    }

    override fun areChildItemsTheSame(oldItem: StakingProxyRvItem, newItem: StakingProxyRvItem): Boolean {
        return oldItem.accountAddress == newItem.accountAddress
    }

    override fun areChildContentsTheSame(oldItem: StakingProxyRvItem, newItem: StakingProxyRvItem): Boolean {
        return true
    }
}

class StakingProxyGroupHolder(
    containerView: View,
) : GroupedListHolder(containerView) {

    fun bind(item: StakingProxyGroupRvItem) = with(containerView) {
        itemProxyGroup.text = item.text
    }
}

class StakingProxyHolder(
    private val eventHandler: StakingProxyListAdapter.Handler,
    private val imageLoader: ImageLoader,
    containerView: View,
) : GroupedListHolder(containerView) {

    fun bind(item: StakingProxyRvItem) = with(containerView) {
        setOnClickListener { eventHandler.onProxyClick(item) }
        itemStakingProxyIcon.setImageDrawable(item.accountIcon)
        itemStakingProxyChainIcon.loadChainIcon(item.chainIconUrl, imageLoader)
        itemStakingProxyAccountTitle.text = item.accountTitle
    }
}
