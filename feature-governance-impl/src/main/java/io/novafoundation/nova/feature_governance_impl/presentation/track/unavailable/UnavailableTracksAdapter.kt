package io.novafoundation.nova.feature_governance_impl.presentation.track.unavailable

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.feature_governance_impl.R
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
            parent.inflateChild(
                R.layout.item_unavailable_tracks_group,
                false
            ),
            handler
        )
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return UnavailableTrackHolder(
            parent.inflateChild(
                R.layout.item_unavailable_track,
                false
            )
        )
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
    containerView: View,
    private val removeVotesHandler: UnavailableTracksAdapter.Handler,
) : GroupedListHolder(containerView) {

    fun bind(item: UnavailableTracksGroupModel) {
        with(itemView) {
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
    containerView: View,
) : GroupedListHolder(containerView) {

    fun bind(item: TrackModel) {
        containerView.itemUnavailableTrack.setTrackModel(item)
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
