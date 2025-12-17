package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.list

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel.NoStakeItem
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view.StakingDashboardNoStakeView

class DashboardNoStakeAdapter(
    private val handler: Handler,
) : ListAdapter<NoStakeItem, DashboardNoStakeViewHolder>(DashboardNoStakeDiffCallback()) {

    interface Handler {

        fun onNoStakeItemClicked(index: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardNoStakeViewHolder {
        return DashboardNoStakeViewHolder(StakingDashboardNoStakeView(parent.context), handler)
    }

    override fun onBindViewHolder(holder: DashboardNoStakeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: DashboardNoStakeViewHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)

        resolvePayload(holder, position, payloads) {
            when (it) {
                NoStakeItem::earnings -> holder.bindEarnings(item)
                NoStakeItem::availableBalance -> holder.bindAvailableBalance(item)
                NoStakeItem::tokenName -> holder.bindTokenName(item)
                NoStakeItem::assetIcon -> holder.bindAssetIcon(item)
                NoStakeItem::stakingTypeBadge -> holder.bindStakingType(item)
            }
        }
    }
}

class DashboardNoStakeViewHolder(
    override val containerView: StakingDashboardNoStakeView,
    private val handler: DashboardNoStakeAdapter.Handler,
) : BaseViewHolder(containerView) {

    init {
        containerView.setOnClickListener { handler.onNoStakeItemClicked(bindingAdapterPosition) }
    }

    fun bind(model: NoStakeItem) {
        bindEarnings(model)
        bindAvailableBalance(model)
        bindTokenName(model)
        bindAssetIcon(model)
        bindStakingType(model)
    }

    fun bindTokenName(model: NoStakeItem) {
        containerView.setTokenName(model.tokenName)
    }

    fun bindEarnings(model: NoStakeItem) {
        containerView.setEarnings(model.earnings)
    }

    fun bindAvailableBalance(model: NoStakeItem) {
        containerView.setAvailableBalance(model.availableBalance)
    }

    fun bindStakingType(model: NoStakeItem) {
        containerView.setStakingTypeBadge(model.stakingTypeBadge)
    }

    fun bindAssetIcon(model: NoStakeItem) {
        containerView.setAssetIcon(model.assetIcon)
    }

    override fun unbind() {
        containerView.unbind()
    }
}

private class DashboardNoStakeDiffCallback : DiffUtil.ItemCallback<NoStakeItem>() {

    private val payloadGenerator = PayloadGenerator(
        NoStakeItem::earnings,
        NoStakeItem::availableBalance,
        NoStakeItem::tokenName,
        NoStakeItem::assetIcon,
        NoStakeItem::stakingTypeBadge
    )

    override fun areItemsTheSame(oldItem: NoStakeItem, newItem: NoStakeItem): Boolean {
        return oldItem.assetId == newItem.assetId
    }

    override fun areContentsTheSame(oldItem: NoStakeItem, newItem: NoStakeItem): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: NoStakeItem, newItem: NoStakeItem): Any? {
        return payloadGenerator.diff(oldItem, newItem)
    }
}
