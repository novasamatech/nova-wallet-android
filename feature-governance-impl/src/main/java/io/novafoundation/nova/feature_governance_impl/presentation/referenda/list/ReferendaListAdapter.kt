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
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlurDrawable
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendaStatusModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumStatus
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumTrack
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumVoting
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.YourVote
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.setModel
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
        bindChild(holder, child)
    }
}

private object CrowdloanDiffCallback : BaseGroupedDiffCallback<ReferendaStatusModel, ReferendumModel>(ReferendaStatusModel::class.java) {

    override fun areGroupItemsTheSame(oldItem: ReferendaStatusModel, newItem: ReferendaStatusModel): Boolean {
        return oldItem.status == newItem.status
    }

    override fun areGroupContentsTheSame(oldItem: ReferendaStatusModel, newItem: ReferendaStatusModel): Boolean {
        return oldItem == newItem
    }

    override fun areChildItemsTheSame(oldItem: ReferendumModel, newItem: ReferendumModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areChildContentsTheSame(oldItem: ReferendumModel, newItem: ReferendumModel): Boolean {
        return oldItem == newItem
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
            containerView.itemReferendumTrack.background = addRipple(getRoundedCornerDrawable(R.color.white_8, cornerSizeInDp = 8))
            containerView.itemReferendumNumber.background = addRipple(getRoundedCornerDrawable(R.color.white_8, cornerSizeInDp = 8))
        }
    }

    fun bind(
        item: ReferendumModel,
    ) = with(containerView) {
        itemReferendumName.text = item.name
        setStatus(item.status)
        setTimeEstimation(item.timeEstimation)
        setTrackAndNumber(item.track, item.number)
        setVoting(item.voting)
        setYourVote(item.yourVote)
    }

    private fun setStatus(status: ReferendumStatus) = with(containerView) {
        itemReferendumStatus.text = status.name
        itemReferendumStatus.setTextColorRes(status.colorRes)
    }

    private fun setTimeEstimation(timeEstimation: ReferendumTimeEstimation?) = with(containerView) {
        itemReferendumTimeEstimate.isVisible = itemReferendumTimeEstimate != null
        if (timeEstimation != null) {
            itemReferendumTimeEstimate.text = timeEstimation.time
            itemReferendumTimeEstimate.setTextColorRes(timeEstimation.colorRes)
            itemReferendumTimeEstimate.setDrawableEnd(timeEstimation.iconRes, widthInDp = 16, paddingInDp = 4, tint = timeEstimation.colorRes)
        }
    }

    private fun setTrackAndNumber(track: ReferendumTrack, number: String) = with(containerView) {
        itemReferendumTrack.text = track.name
        itemReferendumTrack.setDrawableStart(track.iconRes, widthInDp = 16, paddingInDp = 4, tint = R.color.white_64)
        itemReferendumNumber.text = number
    }

    private fun setVoting(voting: ReferendumVoting?) = with(containerView) {
        val hasVotingInfo = voting != null
        itemReferendumThresholdInfo.isVisible = hasVotingInfo
        itemReferendumVotesView.isVisible = hasVotingInfo
        itemReferendumPercentageDetailsGroup.isVisible = hasVotingInfo

        if (voting != null) {
            if (voting.isThresholdReached) {
                itemReferendumThresholdInfo.setDrawableStart(R.drawable.ic_checkmark, widthInDp = 16, paddingInDp = 4, tint = R.color.darkGreen)
            } else {
                itemReferendumThresholdInfo.setDrawableStart(R.drawable.ic_close, widthInDp = 16, paddingInDp = 4, tint = R.color.red)
            }
            itemReferendumThresholdInfo.text = voting.thresholdInfo
            itemReferendumVotesView.setModel(voting)
            itemReferendumPositivePercentage.text = voting.positivePercentage
            itemReferendumThresholdPercentage.text = voting.thresholdPercentage
            itemReferendumNegativePercentage.text = voting.negativePercentage
        }
    }

    private fun setYourVote(vote: YourVote?) = with(containerView) {
        itemReferendumYourVoiceGroup.isVisible = vote != null
        if (vote != null) {
            itemReferendumYourVoteType.text = vote.voteType
            itemReferendumYourVoteType.setTextColorRes(vote.colorRes)
            itemReferendumYourVoteDetails.text = vote.details
        }
    }
}
