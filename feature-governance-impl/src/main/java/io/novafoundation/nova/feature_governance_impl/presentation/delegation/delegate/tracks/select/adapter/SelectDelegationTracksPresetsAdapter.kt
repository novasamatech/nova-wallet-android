package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.tracks.select.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.tracks.select.model.DelegationTracksPresetModel
import kotlinx.android.synthetic.main.item_delegation_tracks_preset.view.itemDelegationTracksPreset

class SelectDelegationTracksPresetsAdapter(
    private val handler: Handler
) : ListAdapter<DelegationTracksPresetModel, DelegationTrackPresetViewHolder>(DelegationTracksPresetDiffCallback) {

    interface Handler {
        fun presetClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelegationTrackPresetViewHolder {
        val containerView = parent.inflateChild(R.layout.item_delegation_tracks_preset)

        return DelegationTrackPresetViewHolder(containerView, handler)
    }

    override fun onBindViewHolder(holder: DelegationTrackPresetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object DelegationTracksPresetDiffCallback : DiffUtil.ItemCallback<DelegationTracksPresetModel>() {
    override fun areItemsTheSame(oldItem: DelegationTracksPresetModel, newItem: DelegationTracksPresetModel): Boolean {
        return oldItem.value == newItem.value
    }

    override fun areContentsTheSame(oldItem: DelegationTracksPresetModel, newItem: DelegationTracksPresetModel): Boolean {
        return true
    }
}

class DelegationTrackPresetViewHolder(
    containerView: View,
    handler: SelectDelegationTracksPresetsAdapter.Handler
) : ViewHolder(containerView) {

    init {
        containerView.setOnClickListener { handler.presetClicked(bindingAdapterPosition) }
    }

    fun bind(item: DelegationTracksPresetModel) {
        with(itemView) {
            itemDelegationTracksPreset.text = item.value
        }
    }
}
