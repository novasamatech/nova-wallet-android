package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_staking_impl.databinding.ItemStartStakingLandingConditionBinding
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.model.StakingConditionRVItem

class StartStakingLandingAdapter : BaseListAdapter<StakingConditionRVItem, StakingConditionViewHolder>(StakingConditionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StakingConditionViewHolder {
        return StakingConditionViewHolder(ItemStartStakingLandingConditionBinding.inflate(parent.inflater(), parent, false))
    }

    override fun onBindViewHolder(holder: StakingConditionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class StakingConditionDiffCallback : DiffUtil.ItemCallback<StakingConditionRVItem>() {

    override fun areItemsTheSame(oldItem: StakingConditionRVItem, newItem: StakingConditionRVItem): Boolean {
        return oldItem.iconId == newItem.iconId
    }

    override fun areContentsTheSame(oldItem: StakingConditionRVItem, newItem: StakingConditionRVItem): Boolean {
        return oldItem == newItem
    }
}

class StakingConditionViewHolder(private val binder: ItemStartStakingLandingConditionBinding) : BaseViewHolder(binder.root) {

    fun bind(item: StakingConditionRVItem) {
        with(binder) {
            itemStakingConditionIcon.setImageResource(item.iconId)
            itemStakingConditionText.text = item.text
        }
    }

    override fun unbind() {
        with(binder) {
            itemStakingConditionIcon.setImageDrawable(null)
            itemStakingConditionText.text = null
        }
    }
}
