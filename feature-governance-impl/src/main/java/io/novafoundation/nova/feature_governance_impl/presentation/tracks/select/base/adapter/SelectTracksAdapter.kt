package io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.base.model.DelegationTrackModel
import kotlinx.android.synthetic.main.item_delegation_track.view.itemDelegationTrack
import kotlinx.android.synthetic.main.item_delegation_track.view.itemDelegationTrackCheckbox

class SelectTracksAdapter(
    private val handler: Handler
) : ListAdapter<DelegationTrackModel, DelegationTrackViewHolder>(DiffCallback) {

    interface Handler {
        fun trackClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelegationTrackViewHolder {
        val containerView = parent.inflateChild(R.layout.item_delegation_track)

        return DelegationTrackViewHolder(containerView, handler)
    }

    override fun onBindViewHolder(holder: DelegationTrackViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: DelegationTrackViewHolder, position: Int, payloads: MutableList<Any>) {
        resolvePayload(holder, position, payloads) {
            when (it) {
                DelegationTrackModel::isSelected -> holder.bindSelected(getItem(position))
            }
        }
    }
}

private val dAppCategoryPayloadGenerator = PayloadGenerator(DelegationTrackModel::isSelected)

private object DiffCallback : DiffUtil.ItemCallback<DelegationTrackModel>() {
    override fun areItemsTheSame(oldItem: DelegationTrackModel, newItem: DelegationTrackModel): Boolean {
        return oldItem.details.name == newItem.details.name
    }

    override fun areContentsTheSame(oldItem: DelegationTrackModel, newItem: DelegationTrackModel): Boolean {
        return oldItem.isSelected == newItem.isSelected
    }

    override fun getChangePayload(oldItem: DelegationTrackModel, newItem: DelegationTrackModel): Any? {
        return dAppCategoryPayloadGenerator.diff(oldItem, newItem)
    }
}

class DelegationTrackViewHolder(
    containerView: View,
    handler: SelectTracksAdapter.Handler
) : ViewHolder(containerView) {

    init {
        containerView.setOnClickListener { handler.trackClicked(bindingAdapterPosition) }
    }

    fun bind(item: DelegationTrackModel) {
        with(itemView) {
            bindSelected(item)
            itemDelegationTrack.setText(item.details.name)
            itemDelegationTrack.setIcon(item.details.icon)
        }
    }

    fun bindSelected(item: DelegationTrackModel) {
        with(itemView) {
            itemDelegationTrackCheckbox.isChecked = item.isSelected
        }
    }
}
