package io.novafoundation.nova.feature_staking_impl.presentation.staking.main

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.model.StakingStoryModel
import kotlinx.android.synthetic.main.item_staking_story.view.stakingStoryItemIcon
import kotlinx.android.synthetic.main.item_staking_story.view.stakingStoryItemTitle

class StakingStoriesAdapter(
    private val itemHandler: StoryItemHandler
) : ListAdapter<StakingStoryModel, StakingStoryHolder>(StoryDiffCallback) {

    interface StoryItemHandler {
        fun storyClicked(story: StakingStoryModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StakingStoryHolder {
        val view = parent.inflateChild(R.layout.item_staking_story)

        return StakingStoryHolder(view)
    }

    override fun onBindViewHolder(holder: StakingStoryHolder, position: Int) {
        holder.bind(getItem(position), itemHandler)
    }
}

class StakingStoryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(story: StakingStoryModel, itemHandler: StakingStoriesAdapter.StoryItemHandler) = with(itemView) {
        stakingStoryItemIcon.text = story.iconSymbol
        stakingStoryItemTitle.setText(story.titleRes)
        setOnClickListener { itemHandler.storyClicked(story) }
    }
}

private object StoryDiffCallback : DiffUtil.ItemCallback<StakingStoryModel>() {

    override fun areItemsTheSame(oldItem: StakingStoryModel, newItem: StakingStoryModel): Boolean {
        return oldItem.titleRes == newItem.titleRes
    }

    override fun areContentsTheSame(oldItem: StakingStoryModel, newItem: StakingStoryModel): Boolean {
        return oldItem == newItem
    }
}
