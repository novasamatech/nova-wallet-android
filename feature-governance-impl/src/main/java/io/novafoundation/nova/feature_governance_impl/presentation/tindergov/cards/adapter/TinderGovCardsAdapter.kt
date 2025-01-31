package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.adapter

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.feature_governance_impl.databinding.ItemTinderGovCardBinding
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.adapter.TinderGovCardsAdapter.Handler

class TinderGovCardsAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val handler: Handler
) : ListAdapter<TinderGovCardRvItem, TinderGovCardViewHolder>(TinderGovCardsDiffCallback()) {

    interface Handler {
        fun onReadMoreClicked(item: TinderGovCardRvItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TinderGovCardViewHolder {
        return TinderGovCardViewHolder(handler, lifecycleOwner, ItemTinderGovCardBinding.inflate(parent.inflater(), parent, false))
    }

    override fun onBindViewHolder(holder: TinderGovCardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class TinderGovCardViewHolder(
    private val handler: Handler,
    lifecycleOwner: LifecycleOwner,
    private val binder: ItemTinderGovCardBinding
) : GroupedListHolder(binder.root) {

    init {
        binder.tinderGovCardReadMore.prepareForProgress(lifecycleOwner)
    }

    fun bind(item: TinderGovCardRvItem) {
        binder.itemTinderGovCardSummary.text = item.summary
        binder.itemTinderGovCardAmountContainer.letOrHide(item.requestedAmount) {
            binder.itemTinderGovCardRequestedAmount.setTextOrHide(it.token)
            binder.itemTinderGovCardRequestedFiat.setTextOrHide(it.fiat)
        }

        binder.tinderGovCardContainer.setBackgroundResource(item.backgroundRes)

        binder.tinderGovCardReadMore.setOnClickListener { handler.onReadMoreClicked(item) }
    }
}

private class TinderGovCardsDiffCallback : DiffUtil.ItemCallback<TinderGovCardRvItem>() {

    override fun areItemsTheSame(oldItem: TinderGovCardRvItem, newItem: TinderGovCardRvItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: TinderGovCardRvItem, newItem: TinderGovCardRvItem): Boolean {
        return oldItem == newItem
    }
}
