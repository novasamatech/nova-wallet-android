package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlurDrawable
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendaStatusModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel
import kotlinx.android.synthetic.main.item_referenda_group.view.*
import kotlinx.android.synthetic.main.item_referendum.view.*

class ReferendaListAdapter(
    private val handler: Handler,
) : GroupedListAdapter<ReferendaStatusModel, ReferendumModel>(CrowdloanDiffCallback) {

    interface Handler {

        fun onReferendaClick()
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return ReferendaGroupHolder(parent.inflateChild(R.layout.item_referenda_group))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return ReferendumChildHolder(handler, parent.inflateChild(R.layout.item_referendum))
    }

    override fun bindGroup(holder: GroupedListHolder, group: ReferendaStatusModel) {
        (holder as ReferendaGroupHolder).bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: ReferendumModel) {
        (holder as ReferendumChildHolder).bind(child)
    }

    override fun bindChild(holder: GroupedListHolder, position: Int, child: ReferendumModel, payloads: List<Any>) {
        resolvePayload(holder, position, payloads) {
        }
    }
}

private object CrowdloanDiffCallback : BaseGroupedDiffCallback<ReferendaStatusModel, ReferendumModel>(ReferendaStatusModel::class.java) {

    override fun areGroupItemsTheSame(oldItem: ReferendaStatusModel, newItem: ReferendaStatusModel): Boolean {
        return false
    }

    override fun areGroupContentsTheSame(oldItem: ReferendaStatusModel, newItem: ReferendaStatusModel): Boolean {
        return false
    }

    override fun areChildItemsTheSame(oldItem: ReferendumModel, newItem: ReferendumModel): Boolean {
        return false
    }

    override fun areChildContentsTheSame(oldItem: ReferendumModel, newItem: ReferendumModel): Boolean {
        return false
    }
}

private class ReferendaGroupHolder(containerView: View) : GroupedListHolder(containerView) {

    fun bind(item: ReferendaStatusModel) = with(containerView) {
        itemView.itemReferendaGroupStatus.text = item.status
        itemView.itemReferendaGroupCounter.text = item.count
    }
}

private class ReferendumChildHolder(
    handler: ReferendaListAdapter.Handler,
    containerView: View,
) : GroupedListHolder(containerView) {

    init {
        with(containerView.context) {
            containerView.background = addRipple(getBlurDrawable())
        }
    }

    fun bind(
        item: ReferendumModel,
    ) = with(containerView) {
        itemReferendumStatus.setText(item.status.nameRes)
        itemReferendumStatus.setTextColorRes(item.status.colorRes)
        item.timeEstimation?.let {
            itemReferendumTimeEstimate.text = it.time.toString()
            itemReferendumTimeEstimate.setTextColorRes(it.colorRes)
            itemReferendumTimeEstimate.setDrawableEnd(it.iconRes)
        }
        itemReferendumName.text = item.name
        item.voting?.let {
            itemReferendumThresholdInfo.text = it.thresholdInfo
            itemReferendumPositivePercentage.text = it.positivePercentage
            itemReferendumThresholdPercentage.text = it.thresholdPercentage
            itemReferendumNegativePercentage.text = it.negativePercentage
        }

        itemReferendumYourVoiceGroup.isVisible = item.yourVote != null
        item.yourVote?.let {
            itemReferendumYourVoteType.setText(it.voteType.typeRes)
            itemReferendumYourVoteType.setTextColorRes(it.voteType.colorRes)
            itemReferendumYourVoteDetails.text = it.yourVoteDetails
        }
    }
}
