package io.novafoundation.nova.feature_push_notifications.data.presentation.governance.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.ImageLoader
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIconToTarget
import io.novafoundation.nova.feature_push_notifications.R
import kotlinx.android.synthetic.main.item_push_governance_settings.view.pushGovernanceItemDelegateVotes
import kotlinx.android.synthetic.main.item_push_governance_settings.view.pushGovernanceItemNewReferenda
import kotlinx.android.synthetic.main.item_push_governance_settings.view.pushGovernanceItemReferendumUpdate
import kotlinx.android.synthetic.main.item_push_governance_settings.view.pushGovernanceItemState
import kotlinx.android.synthetic.main.item_push_governance_settings.view.pushGovernanceItemTracks

class PushGovernanceSettingsAdapter(
    private val imageLoader: ImageLoader,
    private val itemHandler: ItemHandler
) : ListAdapter<PushGovernanceRVItem, PushGovernanceItemViewHolder>(PushGovernanceItemCallback()) {

    interface ItemHandler {
        fun enableSwitcherClick(item: PushGovernanceRVItem)

        fun newReferendaClick(item: PushGovernanceRVItem)

        fun referendaUpdatesClick(item: PushGovernanceRVItem)

        fun delegateVotesClick(item: PushGovernanceRVItem)

        fun tracksClicked(item: PushGovernanceRVItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PushGovernanceItemViewHolder {
        return PushGovernanceItemViewHolder(parent.inflateChild(R.layout.item_push_governance_settings), imageLoader, itemHandler)
    }

    override fun onBindViewHolder(holder: PushGovernanceItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class PushGovernanceItemCallback() : DiffUtil.ItemCallback<PushGovernanceRVItem>() {
    override fun areItemsTheSame(oldItem: PushGovernanceRVItem, newItem: PushGovernanceRVItem): Boolean {
        return oldItem.chainId == newItem.chainId
    }

    override fun areContentsTheSame(oldItem: PushGovernanceRVItem, newItem: PushGovernanceRVItem): Boolean {
        return oldItem == newItem
    }
}

class PushGovernanceItemViewHolder(
    itemView: View,
    private val imageLoader: ImageLoader,
    private val itemHandler: PushGovernanceSettingsAdapter.ItemHandler
) : ViewHolder(itemView) {

    fun bind(item: PushGovernanceRVItem) {
        with(itemView) {
            pushGovernanceItemState.setOnClickListener { itemHandler.enableSwitcherClick(item) }
            pushGovernanceItemNewReferenda.setOnClickListener { itemHandler.newReferendaClick(item) }
            pushGovernanceItemReferendumUpdate.setOnClickListener { itemHandler.referendaUpdatesClick(item) }
            pushGovernanceItemDelegateVotes.setOnClickListener { itemHandler.delegateVotesClick(item) }
            pushGovernanceItemTracks.setOnClickListener { itemHandler.tracksClicked(item) }

            pushGovernanceItemState.setTitle(item.chainName)
            imageLoader.loadChainIconToTarget(item.chainIconUrl, context) {
                pushGovernanceItemState.setIcon(it)
            }

            pushGovernanceItemState.setChecked(item.isEnabled)
            pushGovernanceItemNewReferenda.isVisible = item.isEnabled
            pushGovernanceItemReferendumUpdate.isVisible = item.isEnabled
            pushGovernanceItemDelegateVotes.isVisible = item.isEnabled
            pushGovernanceItemTracks.isVisible = item.isEnabled

            pushGovernanceItemNewReferenda.setChecked(item.isNewReferendaEnabled)
            pushGovernanceItemReferendumUpdate.setChecked(item.isReferendaUpdatesEnabled)
            pushGovernanceItemDelegateVotes.setChecked(item.isDelegationVotesEnabled)
            setTracks(item)
        }
    }

    fun setTracks(item: PushGovernanceRVItem) {
        val tracks = item.tracks
        val value = when (tracks) {
            is PushGovernanceRVItem.Tracks.All -> itemView.context.getString(R.string.common_all)
            is PushGovernanceRVItem.Tracks.Specified -> itemView.context.getString(R.string.selected_tracks_quantity, tracks.items.size, tracks.max)
        }

        itemView.pushGovernanceItemTracks.setValue(value)
    }
}
