package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.list

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.feature_dapp_api.presentation.view.DAppView
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.model.StakingDAppModel

class StakingDappsAdapter(
    private val handler: Handler
) : BaseListAdapter<StakingDAppModel, StakingDappViewHolder>(StakingDappDiffCallback()) {

    interface Handler {

        fun onDAppClicked(item: StakingDAppModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StakingDappViewHolder {
        return StakingDappViewHolder(DAppView.createUsingMathParentWidth(parent.context), handler)
    }

    override fun onBindViewHolder(holder: StakingDappViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private class StakingDappDiffCallback : DiffUtil.ItemCallback<StakingDAppModel>() {

    override fun areItemsTheSame(oldItem: StakingDAppModel, newItem: StakingDAppModel): Boolean {
        return oldItem.url == newItem.url
    }

    override fun areContentsTheSame(oldItem: StakingDAppModel, newItem: StakingDAppModel): Boolean {
        return oldItem == newItem
    }
}

class StakingDappViewHolder(
    private val dAppView: DAppView,
    private val itemHandler: StakingDappsAdapter.Handler,
) : BaseViewHolder(dAppView) {

    fun bind(item: StakingDAppModel) = with(dAppView) {
        setTitle(item.name)
        showSubtitle(false)
        setIconUrl(item.iconUrl)

        setOnClickListener { itemHandler.onDAppClicked(item) }
    }

    override fun unbind() {
        dAppView.clearIcon()
    }
}
