package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.list

import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_staking_impl.databinding.ItemProxyBinding
import io.novafoundation.nova.feature_staking_impl.databinding.ItemProxyGroupBinding
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
        return StakingProxyGroupHolder(ItemProxyGroupBinding.inflate(parent.inflater(), parent, false))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return StakingProxyHolder(handler, imageLoader, ItemProxyBinding.inflate(parent.inflater(), parent, false))
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
    private val binder: ItemProxyGroupBinding,
) : GroupedListHolder(binder.root) {

    fun bind(item: StakingProxyGroupRvItem) = with(binder) {
        itemProxyGroup.text = item.text
    }
}

class StakingProxyHolder(
    private val eventHandler: StakingProxyListAdapter.Handler,
    private val imageLoader: ImageLoader,
    private val binder: ItemProxyBinding,
) : GroupedListHolder(binder.root) {

    fun bind(item: StakingProxyRvItem) = with(binder) {
        root.setOnClickListener { eventHandler.onProxyClick(item) }
        itemStakingProxyIcon.setImageDrawable(item.accountIcon)
        itemStakingProxyChainIcon.loadChainIcon(item.chainIconUrl, imageLoader)
        itemStakingProxyAccountTitle.text = item.accountTitle
    }
}
