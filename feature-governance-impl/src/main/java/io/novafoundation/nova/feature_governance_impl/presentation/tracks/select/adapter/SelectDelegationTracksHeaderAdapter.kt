package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.android.synthetic.main.item_delegation_tracks_header.view.itemDelegationTracksUnavailableTracks
import kotlinx.android.synthetic.main.item_delegation_tracks_header.view.itemDelegationTracksUnavailableTracksText
import kotlinx.android.synthetic.main.item_delegation_tracks_header.view.selectDelegationTracksTitle

class SelectDelegationTracksHeaderAdapter(
    private val handler: Handler
) : RecyclerView.Adapter<DelegationTracksHeaderViewHolder>() {

    interface Handler {
        fun unavailableTracksClicked()
    }

    private var showUnavailableTracks: Boolean = false
    private var title: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelegationTracksHeaderViewHolder {
        val containerView = parent.inflateChild(R.layout.item_delegation_tracks_header)

        return DelegationTracksHeaderViewHolder(containerView, handler)
    }

    override fun getItemCount(): Int {
        return 1
    }

    override fun onBindViewHolder(holder: DelegationTracksHeaderViewHolder, position: Int) {
        holder.bind(showUnavailableTracks, title)
    }

    fun showUnavailableTracks(show: Boolean) {
        showUnavailableTracks = show
        notifyItemChanged(0)
    }

    fun setTitle(title: String) {
        this.title = title
    }
}

class DelegationTracksHeaderViewHolder(
    containerView: View,
    handler: SelectDelegationTracksHeaderAdapter.Handler
) : ViewHolder(containerView) {

    init {
        with(containerView) {
            itemDelegationTracksUnavailableTracksText.setOnClickListener { handler.unavailableTracksClicked() }
        }
    }

    fun bind(showUnavailableTracks: Boolean, title: String?) {
        itemView.itemDelegationTracksUnavailableTracks.isInvisible = !showUnavailableTracks
        itemView.selectDelegationTracksTitle.text = title
    }
}
