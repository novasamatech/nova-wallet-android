package io.novafoundation.nova.feature_staking_impl.presentation.staking.start

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.model.StakingConditionRVItem
import kotlinx.android.synthetic.main.item_start_staking_landing_condition.view.itemStakingConditionIcon
import kotlinx.android.synthetic.main.item_start_staking_landing_condition.view.itemStakingConditionText

class StartStakingLandingAdapter : BaseListAdapter<StakingConditionRVItem, StakingConditionViewHolder>(StakingConditionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StakingConditionViewHolder {
        return StakingConditionViewHolder(parent.inflateChild(R.layout.item_start_staking_landing_condition))
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

class StakingConditionViewHolder(containerView: View) : BaseViewHolder(containerView) {

    fun bind(item: StakingConditionRVItem) {
        with(containerView) {
            itemStakingConditionIcon.setImageResource(item.iconId)
            itemStakingConditionText.text = item.text
        }
    }

    override fun unbind() {
        with(containerView) {
            itemStakingConditionIcon.setImageDrawable(null)
            itemStakingConditionText.text = null
        }
    }
}
