package io.novafoundation.nova.feature_push_notifications.presentation.governance.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.ImageLoader
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIconToTarget
import io.novafoundation.nova.feature_push_notifications.R

class PushGovernanceSettingsAdapter(
    private val imageLoader: ImageLoader,
    private val itemHandler: ItemHandler
) : ListAdapter<PushGovernanceRVItem, PushGovernanceItemViewHolder>(PushGovernanceItemCallback()) {

    interface ItemHandler {
        fun enableSwitcherClick(item: PushGovernanceRVItem)

        fun newReferendaClick(item: PushGovernanceRVItem)

        fun referendaUpdatesClick(item: PushGovernanceRVItem)

        fun tracksClicked(item: PushGovernanceRVItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PushGovernanceItemViewHolder {
        return PushGovernanceItemViewHolder(parent.inflateChild(R.layout.item_push_governance_settings), imageLoader, itemHandler)
    }

    override fun onBindViewHolder(holder: PushGovernanceItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: PushGovernanceItemViewHolder, position: Int, payloads: MutableList<Any>) {
        resolvePayload(holder, position, payloads) {
            val item = getItem(position)
            holder.updateListenners(item)

            when (it) {
                PushGovernanceRVItem::isEnabled -> holder.setEnabled(item)
                PushGovernanceRVItem::isNewReferendaEnabled -> holder.setNewReferendaEnabled(item)
                PushGovernanceRVItem::isReferendaUpdatesEnabled -> holder.setReferendaUpdatesEnabled(item)
                PushGovernanceRVItem::tracksText -> holder.setTracks(item)
            }
        }
    }
}

class PushGovernanceItemCallback() : DiffUtil.ItemCallback<PushGovernanceRVItem>() {
    override fun areItemsTheSame(oldItem: PushGovernanceRVItem, newItem: PushGovernanceRVItem): Boolean {
        return oldItem.chainId == newItem.chainId
    }

    override fun areContentsTheSame(oldItem: PushGovernanceRVItem, newItem: PushGovernanceRVItem): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: PushGovernanceRVItem, newItem: PushGovernanceRVItem): Any? {
        return PushGovernancePayloadGenerator.diff(oldItem, newItem)
    }
}

class PushGovernanceItemViewHolder(
    itemView: View,
    private val imageLoader: ImageLoader,
    private val itemHandler: PushGovernanceSettingsAdapter.ItemHandler
) : ViewHolder(itemView) {

    init {
        itemView.pushGovernanceItemState.setIconTintColor(null)
    }

    fun bind(item: PushGovernanceRVItem) {
        with(itemView) {
            updateListenners(item)

            pushGovernanceItemState.setTitle(item.chainName)
            imageLoader.loadChainIconToTarget(item.chainIconUrl, context) {
                pushGovernanceItemState.setIcon(it)
            }

            setEnabled(item)
            setNewReferendaEnabled(item)
            setReferendaUpdatesEnabled(item)
            setTracks(item)
        }
    }

    fun setTracks(item: PushGovernanceRVItem) {
        itemView.pushGovernanceItemTracks.setValue(item.tracksText)
    }

    fun setEnabled(item: PushGovernanceRVItem) {
        with(itemView) {
            pushGovernanceItemState.setChecked(item.isEnabled)
            pushGovernanceItemNewReferenda.isVisible = item.isEnabled
            pushGovernanceItemReferendumUpdate.isVisible = item.isEnabled
            // pushGovernanceItemDelegateVotes.isVisible = item.isEnabled // currently disabled
            pushGovernanceItemTracks.isVisible = item.isEnabled
        }
    }

    fun setNewReferendaEnabled(item: PushGovernanceRVItem) {
        itemView.pushGovernanceItemNewReferenda.setChecked(item.isNewReferendaEnabled)
    }

    fun setReferendaUpdatesEnabled(item: PushGovernanceRVItem) {
        itemView.pushGovernanceItemReferendumUpdate.setChecked(item.isReferendaUpdatesEnabled)
    }

    fun updateListenners(item: PushGovernanceRVItem) {
        itemView.pushGovernanceItemState.setOnClickListener { itemHandler.enableSwitcherClick(item) }
        itemView.pushGovernanceItemNewReferenda.setOnClickListener { itemHandler.newReferendaClick(item) }
        itemView.pushGovernanceItemReferendumUpdate.setOnClickListener { itemHandler.referendaUpdatesClick(item) }
        itemView.pushGovernanceItemTracks.setOnClickListener { itemHandler.tracksClicked(item) }
    }
}

private object PushGovernancePayloadGenerator : PayloadGenerator<PushGovernanceRVItem>(
    PushGovernanceRVItem::isEnabled,
    PushGovernanceRVItem::isNewReferendaEnabled,
    PushGovernanceRVItem::isReferendaUpdatesEnabled,
    PushGovernanceRVItem::tracksText,
)
