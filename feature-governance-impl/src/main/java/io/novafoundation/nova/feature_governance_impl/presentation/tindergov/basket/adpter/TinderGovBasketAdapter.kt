package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.basket.adpter

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.getDrawableCompat
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.ItemTinderGovBasketBinding

class TinderGovBasketAdapter(
    private val handler: Handler
) : ListAdapter<TinderGovBasketRvItem, TinderGovBasketViewHolder>(TinderGovBasketDiffCallback()) {

    interface Handler {
        fun onItemClicked(item: TinderGovBasketRvItem)

        fun onItemDeleteClicked(item: TinderGovBasketRvItem)
    }

    private var editMode = false

    fun setEditMode(editMode: Boolean) {
        this.editMode = editMode

        notifyItemRangeChanged(0, itemCount, editMode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TinderGovBasketViewHolder {
        return TinderGovBasketViewHolder(handler, ItemTinderGovBasketBinding.inflate(parent.inflater(), parent, false))
    }

    override fun onBindViewHolder(holder: TinderGovBasketViewHolder, position: Int) {
        holder.bind(getItem(position), editMode)
    }
}

class TinderGovBasketViewHolder(
    private val handler: TinderGovBasketAdapter.Handler,
    private val binder: ItemTinderGovBasketBinding
) : GroupedListHolder(binder.root) {

    init {
        itemView.background = containerView.context.getDrawableCompat(R.drawable.bg_primary_list_item)
    }

    fun bind(item: TinderGovBasketRvItem, editMode: Boolean) {
        itemView.setOnClickListener { handler.onItemClicked(item) }

        binder.itemTinderGovBasketDelete.setOnClickListener { handler.onItemDeleteClicked(item) }

        binder.itemTinderGovBasketDelete.isVisible = editMode
        binder.itemTinderGovBasketInfo.isVisible = !editMode
        binder.itemTinderGovBasketId.text = item.idStr
        binder.itemTinderGovBasketTitle.text = item.title
        binder.itemTinderGovBasketSubtitle.text = item.subtitle
    }
}

private class TinderGovBasketDiffCallback : DiffUtil.ItemCallback<TinderGovBasketRvItem>() {

    override fun areItemsTheSame(oldItem: TinderGovBasketRvItem, newItem: TinderGovBasketRvItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: TinderGovBasketRvItem, newItem: TinderGovBasketRvItem): Boolean {
        return oldItem == newItem
    }
}
