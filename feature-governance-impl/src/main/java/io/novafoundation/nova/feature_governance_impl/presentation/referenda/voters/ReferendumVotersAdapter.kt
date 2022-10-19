package io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.setAddressModel
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.model.VoterModel
import kotlinx.android.synthetic.main.item_referendum_voter.view.itemVoterAddress
import kotlinx.android.synthetic.main.item_referendum_voter.view.itemVotesCount
import kotlinx.android.synthetic.main.item_referendum_voter.view.itemVotesCountDetails

class ReferendumVotersAdapter(
    private val handler: Handler,
) : ListAdapter<VoterModel, VoterHolder>(DiffCallback()) {

    interface Handler {

        fun onVoterClick(voter: VoterModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoterHolder {
        val view = parent.inflateChild(R.layout.item_referendum_voter)
        return VoterHolder(handler, view)
    }

    override fun onBindViewHolder(holder: VoterHolder, position: Int) {
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

class VoterHolder(
    private val eventHandler: ReferendumVotersAdapter.Handler,
    containerView: View,
) : GroupedListHolder(containerView) {

    init {
        containerView.setBackgroundResource(R.drawable.bg_primary_list_item)
    }

    fun bind(item: VoterModel) = with(containerView) {
        setOnClickListener { eventHandler.onVoterClick(item) }
        itemVoterAddress.setAddressModel(item.addressModel)
        itemVotesCount.text = item.votesCount
        itemVotesCountDetails.text = item.votesCountDetails
    }
}
