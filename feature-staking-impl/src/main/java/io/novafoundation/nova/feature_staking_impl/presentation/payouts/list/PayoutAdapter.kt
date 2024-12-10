package io.novafoundation.nova.feature_staking_impl.presentation.payouts.list

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ItemListDefaultBinding
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.list.model.PendingPayoutModel

import kotlin.time.ExperimentalTime

class PayoutAdapter(
    private val itemHandler: ItemHandler,
) : ListAdapter<PendingPayoutModel, PayoutViewHolder>(PayoutModelDiffCallback()) {

    interface ItemHandler {
        fun payoutClicked(index: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PayoutViewHolder {
        return PayoutViewHolder(ItemListDefaultBinding.inflate(parent.inflater(), parent, false))
    }

    @ExperimentalTime
    override fun onBindViewHolder(holder: PayoutViewHolder, position: Int) {
        val item = getItem(position)

        holder.bind(item, itemHandler)
    }
}

private class PayoutModelDiffCallback : DiffUtil.ItemCallback<PendingPayoutModel>() {

    override fun areItemsTheSame(oldItem: PendingPayoutModel, newItem: PendingPayoutModel): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(oldItem: PendingPayoutModel, newItem: PendingPayoutModel): Boolean {
        return true
    }
}

class PayoutViewHolder(private val binder: ItemListDefaultBinding) : RecyclerView.ViewHolder(binder.root) {

    @ExperimentalTime
    fun bind(payout: PendingPayoutModel, itemHandler: PayoutAdapter.ItemHandler) = with(binder) {
        with(payout) {
            itemListElementDescriptionLeft.startTimer(timeLeft, createdAt) {
                it.text = binder.root.context.getText(R.string.staking_payout_expired)
                it.setTextColor(binder.root.context.getColor(R.color.text_negative))
            }

            itemListElementTitleLeft.text = validatorTitle
            itemListElementTitleRight.text = amount
            itemListElementDescriptionRight.text = amountFiat
            itemListElementDescriptionLeft.setTextColorRes(daysLeftColor)
        }

        binder.root.setOnClickListener { itemHandler.payoutClicked(bindingAdapterPosition) }
    }
}
