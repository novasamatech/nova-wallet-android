package io.novafoundation.nova.feature_wallet_impl.presentation.transaction.history

import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.formatDateTime
import io.novafoundation.nova.common.utils.formatDaysSinceEpoch
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_wallet_impl.R
import io.novafoundation.nova.feature_wallet_impl.presentation.model.OperationModel
import io.novafoundation.nova.feature_wallet_impl.presentation.model.OperationStatusAppearance
import io.novafoundation.nova.feature_wallet_impl.presentation.transaction.history.model.DayHeader
import kotlinx.android.synthetic.main.item_day_header.view.itemDayHeader
import kotlinx.android.synthetic.main.item_transaction.view.itemTransactionAmount
import kotlinx.android.synthetic.main.item_transaction.view.itemTransactionHeader
import kotlinx.android.synthetic.main.item_transaction.view.itemTransactionIcon
import kotlinx.android.synthetic.main.item_transaction.view.itemTransactionStatus
import kotlinx.android.synthetic.main.item_transaction.view.itemTransactionSubHeader
import kotlinx.android.synthetic.main.item_transaction.view.itemTransactionTime

class TransactionHistoryAdapter(
    private val handler: Handler
) : GroupedListAdapter<DayHeader, OperationModel>(TransactionHistoryDiffCallback) {

    interface Handler {
        fun transactionClicked(transactionModel: OperationModel)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return DayHolder(parent.inflateChild(R.layout.item_day_header))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return TransactionHolder(parent.inflateChild(R.layout.item_transaction))
    }

    override fun bindGroup(holder: GroupedListHolder, group: DayHeader) {
        (holder as DayHolder).bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: OperationModel) {
        (holder as TransactionHolder).bind(child, handler)
    }
}

class TransactionHolder(view: View) : GroupedListHolder(view) {
    fun bind(item: OperationModel, handler: TransactionHistoryAdapter.Handler) {
        with(containerView) {
            with(item) {
                itemTransactionHeader.text = header

                itemTransactionAmount.setTextColorRes(amountColorRes)
                itemTransactionAmount.text = amount

                itemTransactionTime.text = time.formatDateTime(context)

                itemTransactionSubHeader.text = subHeader

                if (statusAppearance != OperationStatusAppearance.COMPLETED) {
                    itemTransactionStatus.makeVisible()
                    itemTransactionStatus.setImageResource(statusAppearance.icon)
                } else {
                    itemTransactionStatus.makeGone()
                }

                setOnClickListener { handler.transactionClicked(this) }
            }

            itemTransactionIcon.setImageDrawable(item.operationIcon)
        }
    }
}

class DayHolder(view: View) : GroupedListHolder(view) {
    fun bind(item: DayHeader) {
        with(containerView) {
            itemDayHeader.text = item.daysSinceEpoch.formatDaysSinceEpoch(context)
        }
    }
}

object TransactionHistoryDiffCallback : BaseGroupedDiffCallback<DayHeader, OperationModel>(DayHeader::class.java) {
    override fun areGroupItemsTheSame(oldItem: DayHeader, newItem: DayHeader): Boolean {
        return oldItem.daysSinceEpoch == oldItem.daysSinceEpoch
    }

    override fun areGroupContentsTheSame(oldItem: DayHeader, newItem: DayHeader): Boolean {
        return true
    }

    override fun areChildItemsTheSame(oldItem: OperationModel, newItem: OperationModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areChildContentsTheSame(oldItem: OperationModel, newItem: OperationModel): Boolean {
        return oldItem.statusAppearance == newItem.statusAppearance &&
            oldItem.header == newItem.header &&
            oldItem.subHeader == newItem.subHeader &&
            oldItem.amount == newItem.amount
    }
}
