package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.view.LayoutInflater
import android.view.ViewGroup
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ItemBittensorRootChoiceBinding
import io.novafoundation.nova.feature_staking_impl.databinding.ItemBittensorSubnetBinding
import io.novafoundation.nova.feature_staking_impl.databinding.ItemBittensorSubnetGroupBinding
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorSubnetIcon
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.displayName

class BittensorSubnetListAdapter(
    private val imageLoader: ImageLoader,
    private val onRootClick: () -> Unit,
    private val onClick: (BittensorSubnetRowUi) -> Unit,
    private val onFavorite: (BittensorSubnetRowUi) -> Unit
) : GroupedListAdapter<BittensorSubnetGroup, BittensorSubnetListItem>(Diff) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position) is BittensorSubnetListItem.Root) TYPE_ROOT else super.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupedListHolder =
        if (viewType == TYPE_ROOT) {
            RootHolder(ItemBittensorRootChoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            super.onCreateViewHolder(parent, viewType)
        }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder =
        GroupHolder(ItemBittensorSubnetGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder =
        SubnetHolder(ItemBittensorSubnetBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun bindGroup(holder: GroupedListHolder, group: BittensorSubnetGroup) = (holder as GroupHolder).bind(group)

    override fun bindChild(holder: GroupedListHolder, child: BittensorSubnetListItem) {
        when (child) {
            is BittensorSubnetListItem.Root -> (holder as RootHolder).bind(child)
            is BittensorSubnetListItem.Subnet -> (holder as SubnetHolder).bind(child.row)
        }
    }

    private class GroupHolder(private val binding: ItemBittensorSubnetGroupBinding) : GroupedListHolder(binding.root) {

        fun bind(group: BittensorSubnetGroup) = with(binding) {
            val isRoot = group == BittensorSubnetGroup.ROOT
            title.setText(if (isRoot) R.string.bittensor_root_staking else R.string.bittensor_subnets_section)
        }
    }

    private inner class RootHolder(private val binding: ItemBittensorRootChoiceBinding) : GroupedListHolder(binding.root) {

        fun bind(item: BittensorSubnetListItem.Root) = with(binding) {
            rootIcon.setIcon(item.icon, imageLoader)
            description.text = root.context.getString(R.string.bittensor_root_row_description, item.tokenSymbol.value)
            root.setOnClickListener { onRootClick() }
        }
    }

    inner class SubnetHolder(private val binding: ItemBittensorSubnetBinding) : GroupedListHolder(binding.root) {

        fun bind(item: BittensorSubnetRowUi) = with(binding) {
            val subnet = item.subnet
            val name = subnet.name.ifBlank { root.context.getString(R.string.bittensor_subnet_number, subnet.id.value) }
            title.text = "\u200e" + subnet.displayName(name)
            price.text = item.price
            logo.load(subnet.logoUrl, imageLoader) {
                bittensorSubnetIcon()
            }
            favorite.setImageResource(if (item.favorite) R.drawable.ic_favorite_heart_filled_20 else R.drawable.ic_favorite_heart_outline_20)
            if (item.favorite) favorite.imageTintList = null else favorite.setImageTintRes(R.color.icon_secondary)
            favoriteAction.contentDescription = root.context.getString(
                if (item.favorite) R.string.bittensor_remove_favorite else R.string.bittensor_add_favorite
            )
            favoriteAction.setOnClickListener { onFavorite(item) }
            root.setOnClickListener { onClick(item) }
        }
    }

    private object Diff : BaseGroupedDiffCallback<BittensorSubnetGroup, BittensorSubnetListItem>(BittensorSubnetGroup::class.java) {

        override fun areGroupItemsTheSame(oldItem: BittensorSubnetGroup, newItem: BittensorSubnetGroup) = oldItem == newItem

        override fun areGroupContentsTheSame(oldItem: BittensorSubnetGroup, newItem: BittensorSubnetGroup) = oldItem == newItem

        override fun areChildItemsTheSame(oldItem: BittensorSubnetListItem, newItem: BittensorSubnetListItem): Boolean = when {
            oldItem is BittensorSubnetListItem.Root && newItem is BittensorSubnetListItem.Root -> true
            oldItem is BittensorSubnetListItem.Subnet && newItem is BittensorSubnetListItem.Subnet ->
                oldItem.row.metaId == newItem.row.metaId && oldItem.row.subnet.id == newItem.row.subnet.id
            else -> false
        }

        override fun areChildContentsTheSame(oldItem: BittensorSubnetListItem, newItem: BittensorSubnetListItem) = oldItem == newItem
    }

    private companion object {
        const val TYPE_ROOT = 3
    }
}
