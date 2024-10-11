package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.delegators.list

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VoterModel

class DelegatorsAdapter(
    private val handler: Handler,
) : ListAdapter<VoterModel, DelegatorHolder>(DiffCallback()) {

    interface Handler {

        fun onVoterClick(voter: VoterModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelegatorHolder {
        val view = parent.inflateChild(R.layout.item_delegator)
        return DelegatorHolder(handler, view)
    }

    override fun onBindViewHolder(holder: DelegatorHolder, position: Int) {
        val voter = getItem(position)
        holder.bind(voter)
    }
}

private class DiffCallback : DiffUtil.ItemCallback<VoterModel>() {
    override fun areItemsTheSame(oldItem: VoterModel, newItem: VoterModel): Boolean {
        return oldItem.addressModel.address == newItem.addressModel.address
    }

    override fun areContentsTheSame(oldItem: VoterModel, newItem: VoterModel): Boolean {
        return true
    }
}

class DelegatorHolder(
    private val eventHandler: DelegatorsAdapter.Handler,
    containerView: View,
) : GroupedListHolder(containerView) {

    init {
        containerView.setBackgroundResource(R.drawable.bg_primary_list_item)
    }

    fun bind(item: VoterModel) = with(containerView) {
        setOnClickListener { eventHandler.onVoterClick(item) }
        itemDelegatorImage.setImageDrawable(item.addressModel.image)
        itemDelegatorAddress.text = item.addressModel.nameOrAddress
        itemDelegatorVotesCount.text = item.vote.votesCount
        itemDelegatorVotesCountDetails.text = item.vote.votesCountDetails
    }
}
