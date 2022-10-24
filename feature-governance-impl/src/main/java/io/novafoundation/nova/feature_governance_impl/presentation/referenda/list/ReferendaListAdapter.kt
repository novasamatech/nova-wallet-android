package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlurDrawable
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumStatusModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.setReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.setReferendumTrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendaGroupModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.YourVotePreviewModel
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
) : GroupedListAdapter<ReferendaGroupModel, ReferendumModel>(ReferendaDiffCallback) {

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

    override fun bindGroup(holder: GroupedListHolder, position: Int, group: ReferendaGroupModel, payloads: List<Any>) {
        bindGroup(holder, group)
    }

    override fun bindChild(holder: GroupedListHolder, position: Int, child: ReferendumModel, payloads: List<Any>) {
        if (holder !is ReferendumChildHolder) return

        resolvePayload(holder, position, payloads) {
            when (it) {
                ReferendumModel::voting -> holder.bindVoting(child)
            }
        }
    }
}

private object ReferendaPayloadGenerator : PayloadGenerator<ReferendumModel>(ReferendumModel::voting)

private object ReferendaDiffCallback : BaseGroupedDiffCallback<ReferendaGroupModel, ReferendumModel>(ReferendaGroupModel::class.java) {

    override fun getGroupChangePayload(oldItem: ReferendaGroupModel, newItem: ReferendaGroupModel): Any? {
        return if (oldItem == newItem) null else true
    }

    override fun getChildChangePayload(oldItem: ReferendumModel, newItem: ReferendumModel): Any? {
        return ReferendaPayloadGenerator.diff(oldItem, newItem)
    }

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
        bindTimeEstimation(item.timeEstimation)
        setTrack(item.track)
        setNumber(item.number)
        bindVoting(item)
        setYourVote(item.yourVote)

        itemView.setOnClickListener { eventHandler.onReferendaClick(item) }
    }

    private fun setStatus(status: ReferendumStatusModel) = with(containerView) {
        itemReferendumStatus.text = status.name
        itemReferendumStatus.setTextColorRes(status.colorRes)
    }

    private fun bindTimeEstimation(timeEstimation: ReferendumTimeEstimation?) = with(containerView) {
        itemReferendumTimeEstimate.setReferendumTimeEstimation(timeEstimation)
    }

    private fun setNumber(number: String) = with(containerView) {
        itemReferendumNumber.text = number
    }

    private fun setTrack(track: ReferendumTrackModel?) = with(containerView) {
        itemReferendumTrack.setReferendumTrackModel(track)
    }

    private fun setYourVote(vote: YourVotePreviewModel?) = with(containerView) {
        itemReferendumYourVoiceGroup.isVisible = vote != null
        if (vote != null) {
            itemReferendumYourVoteType.text = vote.voteType
            itemReferendumYourVoteType.setTextColorRes(vote.colorRes)
            itemReferendumYourVoteDetails.text = vote.details
        }
    }

    fun bindVoting(child: ReferendumModel) = with(containerView) {
        itemReferendumThreshold.setThresholdModel(child.voting)
    }
}
