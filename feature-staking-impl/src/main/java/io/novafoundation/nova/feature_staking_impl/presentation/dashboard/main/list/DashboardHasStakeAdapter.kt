package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.list

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.withRippleMask
import io.novafoundation.nova.common.view.asStyle
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.common.view.shape.getBottomRoundedCornerDrawable
import io.novafoundation.nova.feature_staking_impl.databinding.ItemDashboardHasStakeContainerBinding
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel.HasStakeItem

class DashboardHasStakeAdapter(
    private val handler: Handler,
) : ListAdapter<HasStakeItem, DashboardHasStakeViewHolder>(DashboardHasStakeDiffCallback()) {

    interface Handler {

        fun onHasStakeItemClicked(index: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardHasStakeViewHolder {
        val binder = ItemDashboardHasStakeContainerBinding.inflate(parent.inflater(), parent, false)

        return DashboardHasStakeViewHolder(binder, handler)
    }

    override fun onBindViewHolder(holder: DashboardHasStakeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: DashboardHasStakeViewHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)

        resolvePayload(holder, position, payloads) {
            when (it) {
                HasStakeItem::stake -> holder.bindStake(item)
                HasStakeItem::rewards -> holder.bindRewards(item)
                HasStakeItem::status -> holder.bindStatus(item)
                HasStakeItem::earnings -> holder.bindEarnings(item)
                HasStakeItem::assetIcon -> holder.bindAssetIcon(item)
                HasStakeItem::assetLabel -> holder.bindAssetLabel(item)
                HasStakeItem::stakingTypeBadge -> holder.bindStakingType(item)
                HasStakeItem::announcement -> holder.bindAnnouncement(item)
            }
        }
    }
}

class DashboardHasStakeViewHolder(
    private val binder: ItemDashboardHasStakeContainerBinding,
    private val handler: DashboardHasStakeAdapter.Handler,
) : BaseViewHolder(binder.root) {

    private val card = binder.itemDashboardHasStakeCard

    init {
        card.background = null

        with(binder.root) {
            background = context.getBlockDrawable().withRippleMask()
        }

        binder.root.setOnClickListener { handler.onHasStakeItemClicked(bindingAdapterPosition) }
    }

    fun bind(model: HasStakeItem) {
        bindEarnings(model)
        bindRewards(model)
        bindStake(model)
        bindStatus(model)
        bindAssetIcon(model)
        bindAssetLabel(model)
        bindStakingType(model)
        bindAnnouncement(model)
    }

    fun bindAssetIcon(model: HasStakeItem) {
        card.setAssetIcon(model.assetIcon)
    }

    fun bindAssetLabel(model: HasStakeItem) {
        card.setAssetLabel(model.assetLabel)
    }

    fun bindEarnings(model: HasStakeItem) {
        card.setEarnings(model.earnings)
    }

    fun bindStakingType(model: HasStakeItem) {
        card.setStakingTypeBadge(model.stakingTypeBadge)
    }

    fun bindStake(model: HasStakeItem) {
        card.setStake(model.stake)
    }

    fun bindRewards(model: HasStakeItem) {
        card.setRewards(model.rewards)
    }

    fun bindStatus(model: HasStakeItem) {
        card.setStatus(model.status)
    }

    fun bindAnnouncement(model: HasStakeItem) {
        binder.itemDashboardHasStakeAnnouncement.letOrHide(model.announcement) { announcement ->
            val style = announcement.stylePreset.asStyle()

            with(binder) {
                itemDashboardHasStakeAnnouncement.background = root.context.getBottomRoundedCornerDrawable(
                    fillColorRes = style.backgroundColorRes
                )
                itemDashboardHasStakeAnnouncementIcon.setImageResource(style.iconRes)
                itemDashboardHasStakeAnnouncementIcon.setImageTintRes(style.iconTintRes)
                itemDashboardHasStakeAnnouncementText.text = announcement.description
            }
        }
    }
}

private class DashboardHasStakeDiffCallback : DiffUtil.ItemCallback<HasStakeItem>() {

    private val payloadGenerator = PayloadGenerator(
        HasStakeItem::assetLabel,
        HasStakeItem::stake,
        HasStakeItem::earnings,
        HasStakeItem::status,
        HasStakeItem::rewards,
        HasStakeItem::assetIcon,
        HasStakeItem::assetLabel,
        HasStakeItem::stakingTypeBadge,
        HasStakeItem::announcement
    )

    override fun areItemsTheSame(oldItem: HasStakeItem, newItem: HasStakeItem): Boolean {
        return oldItem.assetId == newItem.assetId
    }

    override fun areContentsTheSame(oldItem: HasStakeItem, newItem: HasStakeItem): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: HasStakeItem, newItem: HasStakeItem): Any? {
        return payloadGenerator.diff(oldItem, newItem)
    }
}
