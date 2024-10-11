package io.novafoundation.nova.feature_assets.presentation.balance.breakdown

import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.BalanceBreakdownAmount
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.BalanceBreakdownTotal
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

class BalanceBreakdownAdapter : GroupedListAdapter<BalanceBreakdownTotal, BalanceBreakdownAmount>(DiffCallback) {

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return BalanceTotalHolder(parent.inflateChild(R.layout.item_balance_breakdown_total, false))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return BalanceAmountHolder(parent.inflateChild(R.layout.item_balance_breakdown_amount, false))
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
    containerView: View,
) : GroupedListHolder(containerView) {

    fun bind(item: BalanceBreakdownTotal) {
        itemView.itemBreakdownTotalIcon.setImageResource(item.iconRes)
        itemView.itemBreakdownTotalName.text = item.name
        itemView.itemBreakdownTotalPercentage.text = item.percentage
        itemView.itemBreakdownTotal.text = item.fiatAmount
    }
}

class BalanceAmountHolder(
    containerView: View,
) : GroupedListHolder(containerView) {

    fun bind(item: BalanceBreakdownAmount) {
        itemView.balanceBreakdownItemDetail.setTitle(item.name)
        itemView.balanceBreakdownItemDetail.showAmount(item.amount)
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
