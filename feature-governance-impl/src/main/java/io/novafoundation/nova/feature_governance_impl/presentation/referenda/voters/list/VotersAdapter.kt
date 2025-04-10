package io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.list

import android.view.ViewGroup
import androidx.core.view.isVisible
import coil.ImageLoader
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.ItemReferendumVoterBinding
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.nameOrAddress
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.setDelegateIcon
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.setDelegateTypeModelIcon

class VotersAdapter(
    private val handler: Handler,
    private val imageLoader: ImageLoader
) : GroupedListAdapter<ExpandableVoterRVItem, DelegatorVoterRVItem>(DiffCallback()) {

    interface Handler {

        fun onVoterClick(position: Int)

        fun onExpandItemClick(position: Int)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return ExpandableVoterHolder(handler, imageLoader, ItemReferendumVoterBinding.inflate(parent.inflater(), parent, false))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return VoterDelegatorHolder(handler, imageLoader, ItemReferendumVoterBinding.inflate(parent.inflater(), parent, false))
    }

    override fun bindGroup(holder: GroupedListHolder, group: ExpandableVoterRVItem) {
        require(holder is ExpandableVoterHolder)
        holder.bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: DelegatorVoterRVItem) {
        require(holder is VoterDelegatorHolder)
        holder.bind(child)
    }

    override fun bindGroup(holder: GroupedListHolder, position: Int, group: ExpandableVoterRVItem, payloads: List<Any>) {
        require(holder is ExpandableVoterHolder)

        resolvePayload(holder, position, payloads) {
            when (it) {
                ExpandableVoterRVItem::isExpanded -> holder.bindExpanding(group)
            }
        }
    }
}

private object VoterPayloadGenerator : PayloadGenerator<ExpandableVoterRVItem>(ExpandableVoterRVItem::isExpanded)

private class DiffCallback : BaseGroupedDiffCallback<ExpandableVoterRVItem, DelegatorVoterRVItem>(ExpandableVoterRVItem::class.java) {
    override fun areGroupItemsTheSame(oldItem: ExpandableVoterRVItem, newItem: ExpandableVoterRVItem): Boolean {
        return oldItem.metadata.address == newItem.metadata.address
    }

    override fun areGroupContentsTheSame(oldItem: ExpandableVoterRVItem, newItem: ExpandableVoterRVItem): Boolean {
        return oldItem.isExpanded == newItem.isExpanded
    }

    override fun areChildItemsTheSame(oldItem: DelegatorVoterRVItem, newItem: DelegatorVoterRVItem): Boolean {
        return oldItem.metadata.address == newItem.metadata.address
    }

    override fun areChildContentsTheSame(oldItem: DelegatorVoterRVItem, newItem: DelegatorVoterRVItem): Boolean {
        return true
    }

    override fun getGroupChangePayload(oldItem: ExpandableVoterRVItem, newItem: ExpandableVoterRVItem): Any? {
        return VoterPayloadGenerator.diff(oldItem, newItem)
    }
}

class ExpandableVoterHolder(
    private val eventHandler: VotersAdapter.Handler,
    private val imageLoader: ImageLoader,
    private val binder: ItemReferendumVoterBinding,
) : GroupedListHolder(binder.root) {

    init {
        containerView.setBackgroundResource(R.drawable.bg_primary_list_item)
        binder.itemVoterAddressContainer.setOnClickListener { eventHandler.onVoterClick(absoluteAdapterPosition) }
    }

    fun bind(item: ExpandableVoterRVItem) = with(binder) {
        itemVoterChevron.isVisible = item.isExpandable

        if (item.isExpandable) {
            itemVoterAddressContainer.background = containerView.context.addRipple()
            itemVoterAddressContainer.isClickable = true
            containerView.setOnClickListener { eventHandler.onExpandItemClick(absoluteAdapterPosition) }
            bindExpanding(item)
        } else {
            itemVoterAddressContainer.background = null
            itemVoterAddressContainer.isClickable = false
            containerView.setOnClickListener { eventHandler.onVoterClick(absoluteAdapterPosition) }
        }

        val delegateIcon = item.metadata.icon
        itemVoterImage.setDelegateIcon(delegateIcon, imageLoader, 4)
        itemVoterType.setDelegateTypeModelIcon(item.metadata.type)
        itemVoterAddress.text = item.metadata.nameOrAddress()
        itemVoterAddress.ellipsize = item.addressEllipsize
        itemVoterAddress.requestLayout()
        itemVotesCount.text = item.vote.votesCount
        itemVotesCountDetails.setTextOrHide(item.vote.votesCountDetails)
    }

    fun bindExpanding(item: ExpandableVoterRVItem) = with(binder) {
        if (item.isExpandable) {
            if (item.isExpanded) {
                itemVoterChevron.setImageResource(R.drawable.ic_chevron_up)
            } else {
                itemVoterChevron.setImageResource(R.drawable.ic_chevron_down)
            }
        } else {
            itemVoterChevron.setImageDrawable(null)
        }
    }
}

class VoterDelegatorHolder(
    private val eventHandler: VotersAdapter.Handler,
    private val imageLoader: ImageLoader,
    private val binder: ItemReferendumVoterBinding,
) : GroupedListHolder(binder.root) {

    init {
        with(binder) {
            containerView.setBackgroundResource(R.drawable.bg_primary_list_item)
            itemVoterChevron.makeGone()
            containerView.setOnClickListener { eventHandler.onVoterClick(absoluteAdapterPosition) }
        }
    }

    fun bind(item: DelegatorVoterRVItem) = with(binder) {
        val delegateIcon = item.metadata.icon
        itemVoterImage.setDelegateIcon(delegateIcon, imageLoader, 4)

        itemVoterType.setDelegateTypeModelIcon(item.metadata.type)
        itemVoterAddress.text = item.metadata.nameOrAddress()
        itemVoterAddress.ellipsize = item.addressEllipsize
        itemVoterAddress.requestLayout()
        itemVotesCount.text = item.vote.votesCount
        itemVotesCountDetails.text = item.vote.votesCountDetails
    }
}
