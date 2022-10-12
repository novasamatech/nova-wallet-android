package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlurDrawable
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.common.view.stopTimer
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendaGroupModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumStatusModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumTrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumVotingModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.YourVoteModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.setModel
import kotlinx.android.synthetic.main.item_referenda_group.view.itemReferendaGroupCounter
import kotlinx.android.synthetic.main.item_referenda_group.view.itemReferendaGroupStatus
import kotlinx.android.synthetic.main.item_referendum.view.itemReferendumName
import kotlinx.android.synthetic.main.item_referendum.view.itemReferendumNumber
import kotlinx.android.synthetic.main.item_referendum.view.itemReferendumStatus
import kotlinx.android.synthetic.main.item_referendum.view.itemReferendumThreshold
import kotlinx.android.synthetic.main.item_referendum.view.itemReferendumTimeEstimate
import kotlinx.android.synthetic.main.item_referendum.view.itemReferendumTrack
import kotlinx.android.synthetic.main.item_referendum.view.itemReferendumYourVoiceGroup
import kotlinx.android.synthetic.main.item_referendum.view.itemReferendumYourVoteDetails
import kotlinx.android.synthetic.main.item_referendum.view.itemReferendumYourVoteType

class ReferendaListAdapter(
    private val handler: Handler,
) : GroupedListAdapter<ReferendaGroupModel, ReferendumModel>(CrowdloanDiffCallback) {

    interface Handler {

        fun onReferendaClick(referendum: ReferendumModel)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return ReferendaGroupHolder(parent.inflateChild(R.layout.item_referenda_group))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return ReferendumChildHolder(handler, parent.inflateChild(R.layout.item_referendum))
    }

    override fun bindGroup(holder: GroupedListHolder, group: ReferendaGroupModel) {
        (holder as ReferendaGroupHolder).bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: ReferendumModel) {
        (holder as ReferendumChildHolder).bind(child)
    }

    override fun bindChild(holder: GroupedListHolder, position: Int, child: ReferendumModel, payloads: List<Any>) {
        bindChild(holder, child)
    }
}

private object CrowdloanDiffCallback : BaseGroupedDiffCallback<ReferendaGroupModel, ReferendumModel>(ReferendaGroupModel::class.java) {

    override fun areGroupItemsTheSame(oldItem: ReferendaGroupModel, newItem: ReferendaGroupModel): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areGroupContentsTheSame(oldItem: ReferendaGroupModel, newItem: ReferendaGroupModel): Boolean {
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

    fun bind(item: ReferendaGroupModel) = with(containerView) {
        itemView.itemReferendaGroupStatus.text = item.name
        itemView.itemReferendaGroupCounter.text = item.badge
    }
}

private class ReferendumChildHolder(
    private val eventHandler: ReferendaListAdapter.Handler,
    containerView: View,
) : GroupedListHolder(containerView) {

    init {
        with(containerView.context) {
            containerView.background = addRipple(getBlurDrawable())
            containerView.itemReferendumTrack.background = addRipple(getRoundedCornerDrawable(R.color.white_8, cornerSizeInDp = 8))
            containerView.itemReferendumNumber.background = addRipple(getRoundedCornerDrawable(R.color.white_8, cornerSizeInDp = 8))
        }
    }

    fun bind(item: ReferendumModel) = with(containerView) {
        itemReferendumName.text = item.name
        setStatus(item.status)
        setTimeEstimation(item.timeEstimation)
        setTrack(item.track)
        setNumber(item.number)
        setVoting(item.voting)
        setYourVote(item.yourVote)

        itemView.setOnClickListener { eventHandler.onReferendaClick(item) }
    }

    private fun setStatus(status: ReferendumStatusModel) = with(containerView) {
        itemReferendumStatus.text = status.name
        itemReferendumStatus.setTextColorRes(status.colorRes)
    }

    private fun setTimeEstimation(timeEstimation: ReferendumTimeEstimation?) = with(containerView) {
        if (timeEstimation == null) {
            itemReferendumTimeEstimate.makeGone()
            return@with
        }

        itemReferendumTimeEstimate.makeVisible()

        when (timeEstimation) {
            is ReferendumTimeEstimation.Text -> {
                itemReferendumTimeEstimate.stopTimer()

                itemReferendumTimeEstimate.text = timeEstimation.text
                itemReferendumTimeEstimate.setReferendumTextStyle(timeEstimation.textStyle)
            }

            is ReferendumTimeEstimation.Timer -> {
                itemReferendumTimeEstimate.setReferendumTextStyle(timeEstimation.textStyleRefresher())

                itemReferendumTimeEstimate.startTimer(
                    value = timeEstimation.time,
                    customMessageFormat = timeEstimation.timeFormat,
                    onTick = { view, _ ->
                        view.setReferendumTextStyle(timeEstimation.textStyleRefresher())
                    }
                )
            }
        }
    }

    private fun TextView.setReferendumTextStyle(textStyle: ReferendumTimeEstimation.TextStyle) {
        setTextColorRes(textStyle.colorRes)
        setDrawableEnd(textStyle.iconRes, widthInDp = 16, paddingInDp = 4, tint = textStyle.colorRes)
    }

    private fun setNumber(number: String) = with(containerView) {
        itemReferendumNumber.text = number
    }

    private fun setTrack(track: ReferendumTrackModel?) = with(containerView) {
        itemReferendumTrack.setVisible(track != null)

        if (track != null) {
            itemReferendumTrack.text = track.name
            itemReferendumTrack.setDrawableStart(track.iconRes, widthInDp = 16, paddingInDp = 4, tint = R.color.white_64)
        }
    }

    private fun setVoting(voting: ReferendumVotingModel?) = with(containerView) {
        val hasVotingInfo = voting != null
        itemReferendumThreshold.isVisible = hasVotingInfo

        if (voting != null) {
            itemReferendumThreshold.setModel(voting)
        }
    }

    private fun setYourVote(vote: YourVoteModel?) = with(containerView) {
        itemReferendumYourVoiceGroup.isVisible = vote != null
        if (vote != null) {
            itemReferendumYourVoteType.text = vote.voteType
            itemReferendumYourVoteType.setTextColorRes(vote.colorRes)
            itemReferendumYourVoteDetails.text = vote.details
        }
    }
}
