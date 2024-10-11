package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.adapter

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getRippleMask
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.setDelegateIcon
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.setDelegateTypeModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.DelegateListModel
import io.novafoundation.nova.feature_governance_impl.presentation.track.setTrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.view.setTextOrHide

class DelegateListAdapter(
    private val imageLoader: ImageLoader,
    private val handler: Handler
) : BaseListAdapter<DelegateListModel, DelegateViewHolder>(DelegateDiffCallback()) {

    interface Handler {

        fun itemClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelegateViewHolder {
        val containerView = parent.inflateChild(R.layout.item_delegate)

        return DelegateViewHolder(containerView, imageLoader, handler)
    }

    override fun onBindViewHolder(holder: DelegateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private class DelegateDiffCallback : ItemCallback<DelegateListModel>() {

    override fun areItemsTheSame(oldItem: DelegateListModel, newItem: DelegateListModel): Boolean {
        return oldItem.accountId.contentEquals(newItem.accountId)
    }

    override fun areContentsTheSame(oldItem: DelegateListModel, newItem: DelegateListModel): Boolean {
        return oldItem.stats == newItem.stats &&
            oldItem.delegation == newItem.delegation
    }
}

class DelegateViewHolder(
    containerView: View,
    private val imageLoader: ImageLoader,
    handler: DelegateListAdapter.Handler
) : BaseViewHolder(containerView) {

    init {
        with(containerView) {
            setOnClickListener { handler.itemClicked(bindingAdapterPosition) }

            itemDelegateCardView.foreground = with(context) { addRipple(mask = getRippleMask(0)) }
        }
    }

    fun bind(model: DelegateListModel) = with(containerView) {
        itemDelegateIcon.setDelegateIcon(icon = model.icon, imageLoader = imageLoader, squareCornerRadiusDp = 8)
        itemDelegateTitle.text = model.name
        itemDelegateDescription.setTextOrHide(model.description)
        itemDelegateStatsGroup.isVisible = model.stats != null
        if (model.stats != null) {
            itemDelegateDelegations.text = model.stats.delegations
            itemDelegateDelegatedVotes.text = model.stats.delegatedVotes
            itemDelegateRecentVotes.text = model.stats.recentVotes.value
            itemDelegateRecentVotesLabel.text = model.stats.recentVotes.label
        }
        itemDelegateType.setDelegateTypeModel(model.type)

        val delegation = model.delegation
        itemDelegateVotedBlock.isVisible = delegation != null
        if (delegation != null) {
            itemVotedTrack.setTrackModel(delegation.firstTrack)
            itemVotedTracksCount.setTextOrHide(delegation.otherTracksCount)
            itemDelegateVotesDetails.isVisible = delegation.votes != null
            if (delegation.votes != null) {
                itemDelegateVotes.text = delegation.votes.votesCount
                itemDelegateConvictionAmount.text = delegation.votes.votesCountDetails
            }
        }
    }

    override fun unbind() = with(containerView) {
        itemDelegateIcon.clear()
    }
}
