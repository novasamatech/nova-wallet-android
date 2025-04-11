package io.novafoundation.nova.feature_staking_impl.presentation.pools.common

import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_staking_impl.databinding.ItemPoolBinding

class PoolAdapter(
    private val imageLoader: ImageLoader,
    private val itemHandler: ItemHandler
) : ListAdapter<PoolRvItem, PoolViewHolder>(PoolDiffCallback()) {

    interface ItemHandler {

        fun poolInfoClicked(poolItem: PoolRvItem)

        fun poolClicked(poolItem: PoolRvItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PoolViewHolder {
        return PoolViewHolder(ItemPoolBinding.inflate(parent.inflater(), parent, false), imageLoader, itemHandler)
    }

    override fun onBindViewHolder(holder: PoolViewHolder, position: Int) {
        val item = getItem(position)

        holder.bind(item)
    }
}

class PoolViewHolder(
    private val binder: ItemPoolBinding,
    private val imageLoader: ImageLoader,
    private val itemHandler: PoolAdapter.ItemHandler
) : RecyclerView.ViewHolder(binder.root) {

    fun bind(poolItem: PoolRvItem) = with(binder) {
        itemPoolTitle.text = poolItem.model.title
        itemPoolSubtitle.text = poolItem.subtitle
        itemPoolMembersCount.text = poolItem.members
        itemPoolIcon.setIcon(poolItem.model.icon, imageLoader)

        itemPoolIcon.isInvisible = poolItem.isChecked
        itemPoolCheckBox.isVisible = poolItem.isChecked

        itemPoolInfo.setOnClickListener {
            itemHandler.poolInfoClicked(poolItem)
        }

        binder.root.setOnClickListener {
            itemHandler.poolClicked(poolItem)
        }
    }
}

class PoolDiffCallback : DiffUtil.ItemCallback<PoolRvItem>() {

    override fun areItemsTheSame(oldItem: PoolRvItem, newItem: PoolRvItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PoolRvItem, newItem: PoolRvItem): Boolean {
        return oldItem.members == newItem.members && oldItem.isChecked == newItem.isChecked
    }
}
