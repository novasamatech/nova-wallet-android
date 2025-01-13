package io.novafoundation.nova.feature_governance_impl.presentation.track.unavailable

import android.view.ViewGroup
import androidx.core.view.isVisible
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.feature_governance_impl.databinding.ItemUnavailableTrackBinding
import io.novafoundation.nova.feature_governance_impl.databinding.ItemUnavailableTracksGroupBinding
import io.novafoundation.nova.feature_governance_impl.presentation.track.TrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.track.setTrackModel

class UnavailableTracksAdapter(
    private val handler: Handler
) : GroupedListAdapter<UnavailableTracksGroupModel, TrackModel>(DiffCallback) {

    interface Handler {
        fun removeVotesClicked()
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return UnavailableTracksGroupHolder(
            ItemUnavailableTracksGroupBinding.inflate(parent.inflater(), parent, false),
            handler
        )
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return UnavailableTrackHolder(ItemUnavailableTrackBinding.inflate(parent.inflater(), parent, false))
    }

    override fun bindGroup(holder: GroupedListHolder, group: UnavailableTracksGroupModel) {
        require(holder is UnavailableTracksGroupHolder)
        holder.bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: TrackModel) {
        require(holder is UnavailableTrackHolder)
        holder.bind(child)
    }
}

class UnavailableTracksGroupHolder(
    private val binder: ItemUnavailableTracksGroupBinding,
    private val removeVotesHandler: UnavailableTracksAdapter.Handler,
) : GroupedListHolder(binder.root) {

    fun bind(item: UnavailableTracksGroupModel) {
        with(binder) {
            itemUnavailableTrackGroupTitle.setText(item.textRes)
            itemUnavailableTrackGroupTitle.updatePadding(bottom = getTitleBottomPadding(item))
            itemUnavailableTrackGroupButton.isVisible = item.showRemoveTracksButton
            itemUnavailableTrackGroupButton.setOnClickListener { removeVotesHandler.removeVotesClicked() }
        }
    }

    private fun getTitleBottomPadding(item: UnavailableTracksGroupModel): Int {
        return if (item.showRemoveTracksButton) {
            0
        } else {
            8.dp(itemView.context)
        }
    }
}

class UnavailableTrackHolder(
    private val binder: ItemUnavailableTrackBinding,
) : GroupedListHolder(binder.root) {

    fun bind(item: TrackModel) {
        binder.itemUnavailableTrack.setTrackModel(item)
    }
}

private object DiffCallback : BaseGroupedDiffCallback<UnavailableTracksGroupModel, TrackModel>(UnavailableTracksGroupModel::class.java) {

    override fun areGroupItemsTheSame(oldItem: UnavailableTracksGroupModel, newItem: UnavailableTracksGroupModel): Boolean {
        return oldItem.textRes == newItem.textRes
    }

    override fun areGroupContentsTheSame(oldItem: UnavailableTracksGroupModel, newItem: UnavailableTracksGroupModel): Boolean {
        return true
    }

    override fun areChildItemsTheSame(oldItem: TrackModel, newItem: TrackModel): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areChildContentsTheSame(oldItem: TrackModel, newItem: TrackModel): Boolean {
        return true
    }
}
