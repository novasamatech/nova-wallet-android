package io.novafoundation.nova.feature_assets.presentation.balance.breakdown

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.BalanceBreakdownAmount
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.BalanceBreakdownItem
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.BalanceBreakdownTotal
import java.security.InvalidParameterException
import kotlinx.android.synthetic.main.item_balance_breakdown_amount.view.itemBreakdownAmount
import kotlinx.android.synthetic.main.item_balance_breakdown_amount.view.itemBreakdownAmountName
import kotlinx.android.synthetic.main.item_balance_breakdown_total.view.itemBreakdownTotal
import kotlinx.android.synthetic.main.item_balance_breakdown_total.view.itemBreakdownTotalIcon
import kotlinx.android.synthetic.main.item_balance_breakdown_total.view.itemBreakdownTotalName
import kotlinx.android.synthetic.main.item_balance_breakdown_total.view.itemBreakdownTotalPercentage

// TODO change to group
class BalanceBreakdownAdapter() : ListAdapter<BalanceBreakdownItem, RecyclerView.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> BalanceTotalHolder(parent.inflateChild(R.layout.item_balance_breakdown_total, false))
            1 -> BalanceAmountHolder(parent.inflateChild(R.layout.item_balance_breakdown_amount, false))
            else -> throw InvalidParameterException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is BalanceTotalHolder) {
            holder.bind(getItem(position))
        } else if (holder is BalanceAmountHolder) {
            holder.bind(getItem(position))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is BalanceBreakdownTotal -> 0
            is BalanceBreakdownAmount -> 1
            else -> throw InvalidParameterException()
        }
    }
}

class BalanceTotalHolder(
    containerView: View,
) : RecyclerView.ViewHolder(containerView) {

    fun bind(item: BalanceBreakdownItem) {
        item as BalanceBreakdownTotal

        itemView.itemBreakdownTotalIcon.setImageResource(item.iconRes)
        itemView.itemBreakdownTotalName.text = item.name
        itemView.itemBreakdownTotalPercentage.text = item.percentage.toString()
        itemView.itemBreakdownTotal.text = item.amount
    }
}

class BalanceAmountHolder(
    containerView: View,
) : RecyclerView.ViewHolder(containerView) {

    fun bind(item: BalanceBreakdownItem) {
        item as BalanceBreakdownAmount

        itemView.itemBreakdownAmountName.text = item.name
        itemView.itemBreakdownAmount.text = item.amount
    }
}

private object DiffCallback : DiffUtil.ItemCallback<BalanceBreakdownItem>() {
    override fun areItemsTheSame(oldItem: BalanceBreakdownItem, newItem: BalanceBreakdownItem): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: BalanceBreakdownItem, newItem: BalanceBreakdownItem): Boolean {
        return true
    }
}
