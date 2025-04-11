package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_governance_impl.databinding.ItemDelegationTracksPresetBinding
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base.model.DelegationTracksPresetModel

class SelectTracksPresetsAdapter(
    private val handler: Handler
) : ListAdapter<DelegationTracksPresetModel, DelegationTrackPresetViewHolder>(TracksPresetDiffCallback) {

    interface Handler {
        fun presetClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelegationTrackPresetViewHolder {
        return DelegationTrackPresetViewHolder(ItemDelegationTracksPresetBinding.inflate(parent.inflater(), parent, false), handler)
    }

    override fun onBindViewHolder(holder: DelegationTrackPresetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object TracksPresetDiffCallback : DiffUtil.ItemCallback<DelegationTracksPresetModel>() {
    override fun areItemsTheSame(oldItem: DelegationTracksPresetModel, newItem: DelegationTracksPresetModel): Boolean {
        return oldItem.label == newItem.label
    }

    override fun areContentsTheSame(oldItem: DelegationTracksPresetModel, newItem: DelegationTracksPresetModel): Boolean {
        return true
    }
}

class DelegationTrackPresetViewHolder(
    private val binder: ItemDelegationTracksPresetBinding,
    handler: SelectTracksPresetsAdapter.Handler
) : ViewHolder(binder.root) {

    init {
        binder.root.setOnClickListener { handler.presetClicked(bindingAdapterPosition) }
    }

    fun bind(item: DelegationTracksPresetModel) {
        with(binder) {
            itemDelegationTracksPreset.text = item.label
        }
    }
}
