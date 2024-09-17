package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.basket.adpter

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.getDrawableCompat
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.android.synthetic.main.item_tinder_gov_basket.view.itemTinderGovBasketDelete
import kotlinx.android.synthetic.main.item_tinder_gov_basket.view.itemTinderGovBasketId
import kotlinx.android.synthetic.main.item_tinder_gov_basket.view.itemTinderGovBasketSubtitle
import kotlinx.android.synthetic.main.item_tinder_gov_basket.view.itemTinderGovBasketTitle

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
        return TinderGovBasketViewHolder(handler, parent.inflateChild(R.layout.item_tinder_gov_basket))
    }

    override fun onBindViewHolder(holder: TinderGovBasketViewHolder, position: Int) {
        holder.bind(getItem(position), editMode)
    }
}

class TinderGovBasketViewHolder(
    private val handler: TinderGovBasketAdapter.Handler,
    containerView: View
) : GroupedListHolder(containerView) {

    init {
        itemView.background = containerView.context.getDrawableCompat(R.drawable.bg_primary_list_item)
    }

    fun bind(item: TinderGovBasketRvItem, editMode: Boolean) {
        itemView.setOnClickListener { handler.onItemClicked(item) }

        itemView.itemTinderGovBasketDelete.setOnClickListener { handler.onItemDeleteClicked(item) }

        itemView.itemTinderGovBasketDelete.isVisible = editMode
        itemView.itemTinderGovBasketId.text = item.idStr
        itemView.itemTinderGovBasketTitle.text = item.title
        itemView.itemTinderGovBasketSubtitle.text = item.subtitle
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
