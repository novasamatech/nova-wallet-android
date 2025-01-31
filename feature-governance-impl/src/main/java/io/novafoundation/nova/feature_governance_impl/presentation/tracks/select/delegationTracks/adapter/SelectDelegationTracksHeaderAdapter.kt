package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.delegationTracks.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_governance_impl.databinding.ItemDelegationTracksHeaderBinding

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
        return DelegationTracksHeaderViewHolder(ItemDelegationTracksHeaderBinding.inflate(parent.inflater(), parent, false), handler)
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
    private val binder: ItemDelegationTracksHeaderBinding,
    handler: SelectTracksHeaderAdapter.Handler
) : ViewHolder(binder.root) {

    init {
        with(binder) {
            itemDelegationTracksUnavailableTracksText.setOnClickListener { handler.unavailableTracksClicked() }
        }
    }

    fun bind(showUnavailableTracks: Boolean, title: String?, showDescription: Boolean) = with(binder) {
        itemDelegationTracksUnavailableTracks.setVisible(showUnavailableTracks)
        selectDelegationTracksTitle.text = title
        itemDelegationTracksDescriptionGroup.setVisible(showDescription)
    }
}
