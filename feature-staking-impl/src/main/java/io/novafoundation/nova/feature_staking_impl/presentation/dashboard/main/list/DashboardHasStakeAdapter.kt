package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.list

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.isSyncing
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel.HasStakeItem
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view.StakingDashboardHasStakeView

class DashboardHasStakeAdapter(
    private val handler: Handler,
) : ListAdapter<HasStakeItem, DashboardHasStakeViewHolder>(DashboardHasStakeDiffCallback()) {

    interface Handler {

        fun onHasStakeItemClicked(index: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardHasStakeViewHolder {
        return DashboardHasStakeViewHolder(StakingDashboardHasStakeView(parent.context), handler)
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
                HasStakeItem::syncingStage -> holder.bindSyncing(item)
            }
        }
    }
}

class DashboardHasStakeViewHolder(
    override val containerView: StakingDashboardHasStakeView,
    private val handler: DashboardHasStakeAdapter.Handler,
) : BaseViewHolder(containerView) {

    init {
        containerView.setOnClickListener { handler.onHasStakeItemClicked(bindingAdapterPosition) }
    }

    fun bind(model: HasStakeItem) {
        bindEarnings(model)
        bindRewards(model)
        bindStake(model)
        bindStatus(model)
        bindSyncing(model)

        containerView.setChainUi(model.chainUi)
    }

    fun bindEarnings(model: HasStakeItem) {
        containerView.setEarnings(model.earnings)
    }

    fun bindStake(model: HasStakeItem) {
        containerView.setStake(model.stake)
    }

    fun bindRewards(model: HasStakeItem) {
        containerView.setRewards(model.rewards)
    }

    fun bindStatus(model: HasStakeItem) {
        containerView.setStatus(model.status)
    }

    fun bindSyncing(model: HasStakeItem) {
        containerView.setSyncing(model.syncingStage.isSyncing())
    }

    override fun unbind() {
        containerView.unbind()
    }
}

private class DashboardHasStakeDiffCallback : DiffUtil.ItemCallback<HasStakeItem>() {

    private val payloadGenerator = PayloadGenerator(
        HasStakeItem::stake,
        HasStakeItem::earnings,
        HasStakeItem::status,
        HasStakeItem::rewards,
        HasStakeItem::syncingStage
    )

    override fun areItemsTheSame(oldItem: HasStakeItem, newItem: HasStakeItem): Boolean {
        return oldItem.chainUi.id == newItem.chainUi.id && oldItem.assetId == newItem.assetId
    }

    override fun areContentsTheSame(oldItem: HasStakeItem, newItem: HasStakeItem): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: HasStakeItem, newItem: HasStakeItem): Any? {
        return payloadGenerator.diff(oldItem, newItem)
    }
}
