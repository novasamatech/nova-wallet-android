package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list

import android.view.Gravity
import android.view.ViewGroup
import androidx.core.view.isGone
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.presentation.masking.dataOrNull
import io.novafoundation.nova.common.presentation.masking.setMaskableText
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.common.view.shape.getRippleMask
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.ItemReferendaGroupBinding
import io.novafoundation.nova.feature_governance_impl.databinding.ItemReferendumBinding
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.setReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.setReferendumTrackModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendaGroupModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel

class ReferendaListAdapter(
    private val handler: Handler,
) : GroupedListAdapter<ReferendaGroupModel, ReferendumModel>(ReferendaDiffCallback) {

    interface Handler {

        fun onReferendaClick(referendum: ReferendumModel)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return ReferendaGroupHolder(ItemReferendaGroupBinding.inflate(parent.inflater(), parent, false))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return ReferendumChildHolder(handler, ItemReferendumBinding.inflate(parent.inflater(), parent, false))
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
                ReferendumModel::name -> holder.bindName(child)
                ReferendumModel::voting -> holder.bindVoting(child)
                ReferendumModel::status -> holder.bindStatus(child)
                ReferendumModel::timeEstimation -> holder.bindTimeEstimation(child)
                ReferendumModel::yourVote -> holder.bindYourVote(child)
                ReferendumModel::track -> holder.bindTrack(child)
                ReferendumModel::number -> holder.bindNumber(child)
            }
        }
    }
}

private object ReferendaPayloadGenerator : PayloadGenerator<ReferendumModel>(
    ReferendumModel::name,
    ReferendumModel::voting,
    ReferendumModel::status,
    ReferendumModel::timeEstimation,
    ReferendumModel::yourVote,
    ReferendumModel::track,
    ReferendumModel::number
)

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

private class ReferendaGroupHolder(private val binder: ItemReferendaGroupBinding) : GroupedListHolder(binder.root) {

    fun bind(item: ReferendaGroupModel) = with(binder) {
        itemReferendaGroupStatus.text = item.name
        itemReferendaGroupCounter.setMaskableText(item.badge)
    }
}

private class ReferendumChildHolder(
    private val eventHandler: ReferendaListAdapter.Handler,
    private val binder: ItemReferendumBinding,
) : GroupedListHolder(binder.root) {

    init {
        with(containerView.context) {
            containerView.background = addRipple(getBlockDrawable())
            binder.itemReferendumTrack.background = addRipple(
                getRoundedCornerDrawable(R.color.chips_background, cornerSizeInDp = 8),
                mask = getRippleMask(cornerSizeDp = 12)
            )
            binder.itemReferendumNumber.background = addRipple(
                getRoundedCornerDrawable(R.color.chips_background, cornerSizeInDp = 8),
                mask = getRippleMask(cornerSizeDp = 12)
            )
        }
    }

    fun bind(item: ReferendumModel) = with(containerView) {
        bindName(item)
        bindTrack(item)
        bindNumber(item)
        bindStatus(item)
        bindTimeEstimation(item)
        bindVoting(item)
        bindYourVote(item)

        itemView.setOnClickListener { eventHandler.onReferendaClick(item) }
    }

    fun bindName(item: ReferendumModel) = with(binder) {
        itemReferendumName.text = item.name
    }

    fun bindStatus(item: ReferendumModel) = with(binder) {
        itemReferendumStatus.text = item.status.name
        itemReferendumStatus.setTextColorRes(item.status.colorRes)
    }

    fun bindTimeEstimation(item: ReferendumModel) = with(binder) {
        itemReferendumTimeEstimate.setReferendumTimeEstimation(item.timeEstimation, Gravity.END)
    }

    fun bindNumber(item: ReferendumModel) = with(binder) {
        itemReferendumNumber.setText(item.number)
    }

    fun bindTrack(item: ReferendumModel) = with(binder) {
        itemReferendumTrack.setReferendumTrackModel(item.track)
    }

    fun bindYourVote(item: ReferendumModel) = with(binder) {
        itemReferendumYourVote.setModel(item.yourVote?.dataOrNull())
        itermReferendumDivider.isGone = item.yourVote?.dataOrNull()?.votes.isNullOrEmpty()
    }

    fun bindVoting(child: ReferendumModel) = with(binder) {
        itemReferendumThreshold.setThresholdModel(child.voting)
    }

    override fun unbind() {
        binder.itemReferendumTrack.clearIcon()
    }
}
