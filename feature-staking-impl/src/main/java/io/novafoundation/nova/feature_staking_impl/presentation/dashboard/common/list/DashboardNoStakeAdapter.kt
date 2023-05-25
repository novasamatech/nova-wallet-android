package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.list

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.isSyncing
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
                NoStakeItem::syncingStage -> holder.bindSyncing(item)
                NoStakeItem::availableBalance -> holder.bindAvailableBalance(item)
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
        bindSyncing(model)
        bindAvailableBalance(model)

        containerView.setChainUi(model.chainUi)
    }

    fun bindEarnings(model: NoStakeItem) {
        containerView.setEarnings(model.earnings)
    }

    fun bindSyncing(model: NoStakeItem) {
        containerView.setSyncing(model.syncingStage.isSyncing())
    }

    fun bindAvailableBalance(model: NoStakeItem) {
        containerView.setAvailableBalance(model.availableBalance)
    }

    override fun unbind() {
        containerView.unbind()
    }
}

private class DashboardNoStakeDiffCallback : DiffUtil.ItemCallback<NoStakeItem>() {

    private val payloadGenerator = PayloadGenerator(NoStakeItem::earnings, NoStakeItem::syncingStage, NoStakeItem::availableBalance)

    override fun areItemsTheSame(oldItem: NoStakeItem, newItem: NoStakeItem): Boolean {
        return oldItem.chainUi.id == newItem.chainUi.id && oldItem.assetId == newItem.assetId
    }

    override fun areContentsTheSame(oldItem: NoStakeItem, newItem: NoStakeItem): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: NoStakeItem, newItem: NoStakeItem): Any? {
        return payloadGenerator.diff(oldItem, newItem)
    }
}
