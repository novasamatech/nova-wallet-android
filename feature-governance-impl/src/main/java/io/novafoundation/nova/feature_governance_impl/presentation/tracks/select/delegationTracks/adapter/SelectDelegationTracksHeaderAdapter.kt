package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.delegationTracks.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.android.synthetic.main.item_delegation_tracks_header.view.itemDelegationTracksDescriptionGroup
import kotlinx.android.synthetic.main.item_delegation_tracks_header.view.itemDelegationTracksUnavailableTracks
import kotlinx.android.synthetic.main.item_delegation_tracks_header.view.itemDelegationTracksUnavailableTracksText
import kotlinx.android.synthetic.main.item_delegation_tracks_header.view.selectDelegationTracksTitle

class SelectTracksHeaderAdapter(
    private val handler: Handler
) : RecyclerView.Adapter<DelegationTracksHeaderViewHolder>() {

    interface Handler {
        fun unavailableTracksClicked()
    }

    private var showUnavailableTracks: Boolean = false
    private var title: String? = null
    private var showDescription: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelegationTracksHeaderViewHolder {
        val containerView = parent.inflateChild(R.layout.item_delegation_tracks_header)

        return DelegationTracksHeaderViewHolder(containerView, handler)
    }

    override fun getItemCount(): Int {
        return 1
    }

    override fun onBindViewHolder(holder: DelegationTracksHeaderViewHolder, position: Int) {
        holder.bind(showUnavailableTracks, title, showDescription)
    }

    fun showUnavailableTracks(show: Boolean) {
        showUnavailableTracks = show
        notifyItemChanged(0)
    }

    fun setTitle(title: String) {
        this.title = title
        notifyItemChanged(0)
    }

    fun setShowDescription(show: Boolean) {
        showDescription = show
        notifyItemChanged(0)
    }
}

class DelegationTracksHeaderViewHolder(
    containerView: View,
    handler: SelectTracksHeaderAdapter.Handler
) : ViewHolder(containerView) {

    init {
        with(containerView) {
            itemDelegationTracksUnavailableTracksText.setOnClickListener { handler.unavailableTracksClicked() }
        }
    }

    fun bind(showUnavailableTracks: Boolean, title: String?, showDescription: Boolean) = with(itemView) {
        itemDelegationTracksUnavailableTracks.setVisible(showUnavailableTracks)
        selectDelegationTracksTitle.text = title
        itemDelegationTracksDescriptionGroup.setVisible(showDescription)
    }
}
