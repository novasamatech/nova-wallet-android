package io.novafoundation.nova.feature_assets.presentation.balance.breakdown

import android.view.ViewGroup
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_assets.databinding.ItemBalanceBreakdownAmountBinding
import io.novafoundation.nova.feature_assets.databinding.ItemBalanceBreakdownTotalBinding
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.BalanceBreakdownAmount
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.BalanceBreakdownTotal
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

class BalanceBreakdownAdapter : GroupedListAdapter<BalanceBreakdownTotal, BalanceBreakdownAmount>(DiffCallback) {

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return BalanceTotalHolder(ItemBalanceBreakdownTotalBinding.inflate(parent.inflater(), parent, false))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return BalanceAmountHolder(ItemBalanceBreakdownAmountBinding.inflate(parent.inflater(), parent, false))
    }

    override fun bindGroup(holder: GroupedListHolder, group: BalanceBreakdownTotal) {
        require(holder is BalanceTotalHolder)
        holder.bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: BalanceBreakdownAmount) {
        require(holder is BalanceAmountHolder)
        holder.bind(child)
    }
}

class BalanceTotalHolder(
    private val binder: ItemBalanceBreakdownTotalBinding,
) : GroupedListHolder(binder.root) {

    fun bind(item: BalanceBreakdownTotal) {
        binder.itemBreakdownTotalIcon.setImageResource(item.iconRes)
        binder.itemBreakdownTotalName.text = item.name
        binder.itemBreakdownTotalPercentage.text = item.percentage
        binder.itemBreakdownTotal.text = item.fiatAmount
    }
}

class BalanceAmountHolder(
    private val binder: ItemBalanceBreakdownAmountBinding,
) : GroupedListHolder(binder.root) {

    fun bind(item: BalanceBreakdownAmount) {
        binder.balanceBreakdownItemDetail.setTitle(item.name)
        binder.balanceBreakdownItemDetail.showAmount(item.amount)
    }
}

private object DiffCallback : BaseGroupedDiffCallback<BalanceBreakdownTotal, BalanceBreakdownAmount>(BalanceBreakdownTotal::class.java) {

    override fun areGroupItemsTheSame(oldItem: BalanceBreakdownTotal, newItem: BalanceBreakdownTotal): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areGroupContentsTheSame(oldItem: BalanceBreakdownTotal, newItem: BalanceBreakdownTotal): Boolean {
        return oldItem == newItem
    }

    override fun areChildItemsTheSame(oldItem: BalanceBreakdownAmount, newItem: BalanceBreakdownAmount): Boolean {
        return oldItem == newItem
    }

    override fun areChildContentsTheSame(oldItem: BalanceBreakdownAmount, newItem: BalanceBreakdownAmount): Boolean {
        return true
    }

    override fun getGroupChangePayload(oldItem: BalanceBreakdownTotal, newItem: BalanceBreakdownTotal): Any? {
        return true
    }
}
