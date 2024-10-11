package io.novafoundation.nova.feature_crowdloan_impl.presentation.main

import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.presentation.main.model.CrowdloanModel
import io.novafoundation.nova.feature_crowdloan_impl.presentation.main.model.CrowdloanStatusModel

class CrowdloanAdapter(
    private val imageLoader: ImageLoader,
    private val handler: Handler,
) : GroupedListAdapter<CrowdloanStatusModel, CrowdloanModel>(CrowdloanDiffCallback) {

    interface Handler {

        fun crowdloanClicked(paraId: ParaId)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return CrowdloanGroupHolder(parent.inflateChild(R.layout.item_crowdloan_group))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return CrowdloanChildHolder(imageLoader, parent.inflateChild(R.layout.item_crowdloan))
    }

    override fun bindGroup(holder: GroupedListHolder, group: CrowdloanStatusModel) {
        (holder as CrowdloanGroupHolder).bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: CrowdloanModel) {
        (holder as CrowdloanChildHolder).bind(child, handler)
    }

    override fun bindChild(holder: GroupedListHolder, position: Int, child: CrowdloanModel, payloads: List<Any>) {
        resolvePayload(holder, position, payloads) {
            when (it) {
                CrowdloanModel::state -> (holder as CrowdloanChildHolder).bindState(child, handler)
                CrowdloanModel::raised -> (holder as CrowdloanChildHolder).bindRaised(child)
            }
        }
    }
}

private object CrowdloanDiffCallback : BaseGroupedDiffCallback<CrowdloanStatusModel, CrowdloanModel>(CrowdloanStatusModel::class.java) {

    override fun getChildChangePayload(oldItem: CrowdloanModel, newItem: CrowdloanModel): Any? {
        return CrowdloanPayloadGenerator.diff(oldItem, newItem)
    }

    override fun areGroupItemsTheSame(oldItem: CrowdloanStatusModel, newItem: CrowdloanStatusModel): Boolean {
        return oldItem == newItem
    }

    override fun areGroupContentsTheSame(oldItem: CrowdloanStatusModel, newItem: CrowdloanStatusModel): Boolean {
        return true
    }

    override fun areChildItemsTheSame(oldItem: CrowdloanModel, newItem: CrowdloanModel): Boolean {
        return oldItem.parachainId == newItem.parachainId && oldItem.relaychainId == newItem.relaychainId
    }

    override fun areChildContentsTheSame(oldItem: CrowdloanModel, newItem: CrowdloanModel): Boolean {
        return oldItem.relaychainId == newItem.relaychainId &&
            oldItem.parachainId == newItem.parachainId &&
            oldItem.title == newItem.title &&
            oldItem.description == newItem.description &&
            oldItem.raised == newItem.raised &&
            oldItem.state == newItem.state
    }
}

private object CrowdloanPayloadGenerator : PayloadGenerator<CrowdloanModel>(
    CrowdloanModel::state,
    CrowdloanModel::raised
)

private class CrowdloanGroupHolder(containerView: View) : GroupedListHolder(containerView) {

    fun bind(item: CrowdloanStatusModel) = with(containerView) {
        itemCrowdloanGroupStatus.text = item.status
        itemCrowdloanGroupCounter.text = item.count
    }
}

private class CrowdloanChildHolder(
    private val imageLoader: ImageLoader,
    containerView: View,
) : GroupedListHolder(containerView) {

    init {
        with(containerView.context) {
            containerView.background = addRipple(getBlockDrawable())
        }
    }

    fun bind(
        item: CrowdloanModel,
        handler: CrowdloanAdapter.Handler,
    ) = with(containerView) {
        itemCrowdloanParaDescription.text = item.description
        itemCrowdloanParaName.text = item.title

        bindRaised(item)

        itemCrowdloanIcon.setIcon(item.icon, imageLoader)

        bindState(item, handler)
    }

    fun bindState(item: CrowdloanModel, handler: CrowdloanAdapter.Handler) = with(containerView) {
        if (item.state is CrowdloanModel.State.Active) {
            itemCrowdloanTimeRemaining.makeVisible()
            itemCrowdloanTimeRemaining.text = item.state.timeRemaining

            itemCrowdloanParaName.setTextColorRes(R.color.text_primary)
            itemCrowdloanParaRaisedPercentage.setTextColorRes(R.color.progress_bar_text)

            itemCrowdloanArrow.makeVisible()

            setOnClickListener { handler.crowdloanClicked(item.parachainId) }

            itemCrowdloanParaRaisedProgress.isEnabled = true
        } else {
            itemCrowdloanTimeRemaining.makeGone()
            itemCrowdloanArrow.makeGone()

            itemCrowdloanParaName.setTextColorRes(R.color.text_secondary)
            itemCrowdloanParaRaisedPercentage.setTextColorRes(R.color.text_secondary)

            itemCrowdloanParaRaisedProgress.isEnabled = false

            setOnClickListener(null)
        }
    }

    override fun unbind() {
        with(containerView) {
            itemCrowdloanIcon.clear()
        }
    }

    fun bindRaised(item: CrowdloanModel) = with(containerView) {
        itemCrowdloanParaRaised.text = item.raised.value
        itemCrowdloanParaRaisedProgress.progress = item.raised.percentage
        itemCrowdloanParaRaisedPercentage.text = item.raised.percentageDisplay
    }
}
