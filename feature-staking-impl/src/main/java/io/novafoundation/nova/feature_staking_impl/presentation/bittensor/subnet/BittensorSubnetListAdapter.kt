package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.isNegative
import io.novafoundation.nova.common.utils.ltrIsolated
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ItemBittensorRootChoiceBinding
import io.novafoundation.nova.feature_staking_impl.databinding.ItemBittensorSubnetBinding
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorSubnetIcon
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.displayName

class BittensorSubnetListAdapter(
    private val imageLoader: ImageLoader,
    private val onRootClick: () -> Unit,
    private val onClick: (BittensorSubnetRowUi) -> Unit,
    private val onFavorite: (BittensorSubnetRowUi) -> Unit
) : ListAdapter<BittensorSubnetListItem, GroupedListHolder>(Diff) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position) is BittensorSubnetListItem.Root) TYPE_ROOT else TYPE_SUBNET

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupedListHolder =
        if (viewType == TYPE_ROOT) {
            RootHolder(ItemBittensorRootChoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            SubnetHolder(ItemBittensorSubnetBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

    override fun onBindViewHolder(holder: GroupedListHolder, position: Int) {
        val child = getItem(position)
        when (child) {
            is BittensorSubnetListItem.Root -> (holder as RootHolder).bind(child)
            is BittensorSubnetListItem.Subnet -> (holder as SubnetHolder).bind(child.row)
        }
    }

    private inner class RootHolder(private val binding: ItemBittensorRootChoiceBinding) : GroupedListHolder(binding.root) {

        fun bind(item: BittensorSubnetListItem.Root) = with(binding) {
            rootIcon.setIcon(item.icon, imageLoader)
            root.setOnClickListener { onRootClick() }
        }
    }

    inner class SubnetHolder(private val binding: ItemBittensorSubnetBinding) : GroupedListHolder(binding.root) {

        fun bind(item: BittensorSubnetRowUi) = with(binding) {
            val subnet = item.subnet
            title.text = subnet.displayName(root.context).ltrIsolated()
            price.text = item.price
            val weeklyChange = item.weeklyChange
            val summary = (weeklyChange as? BittensorSubnetWeeklyChange.Available)?.summary
            change.text = when (weeklyChange) {
                BittensorSubnetWeeklyChange.Loading -> null
                BittensorSubnetWeeklyChange.NotListed -> root.context.getString(R.string.bittensor_no_price_history)
                BittensorSubnetWeeklyChange.Unavailable -> "—"
                is BittensorSubnetWeeklyChange.Available -> root.context.getString(
                    R.string.bittensor_picker_weekly_change,
                    bittensorPickerChangePercent(weeklyChange.summary.changePercent)
                )
            }
            change.setTextColorRes(
                when {
                    summary == null -> R.color.text_secondary
                    summary.changePercent.isNegative -> R.color.text_negative
                    else -> R.color.text_positive
                }
            )
            sparkline.setSummary(summary)
            logo.load(subnet.logoUrl, imageLoader) {
                bittensorSubnetIcon()
            }
            bindFavorite(item.favorite)
            favoriteAction.setOnClickListener { onFavorite(item) }
            root.setOnClickListener { onClick(item) }
        }

        private fun bindFavorite(isFavorite: Boolean) = with(binding) {
            favorite.setImageResource(if (isFavorite) R.drawable.ic_favorite_heart_filled_20 else R.drawable.ic_favorite_heart_outline_20)
            if (isFavorite) favorite.imageTintList = null else favorite.setImageTintRes(R.color.icon_secondary)
            favoriteAction.contentDescription = root.context.getString(
                if (isFavorite) R.string.bittensor_remove_favorite else R.string.bittensor_add_favorite
            )
        }
    }

    private object Diff : DiffUtil.ItemCallback<BittensorSubnetListItem>() {

        override fun areItemsTheSame(oldItem: BittensorSubnetListItem, newItem: BittensorSubnetListItem): Boolean = when {
            oldItem is BittensorSubnetListItem.Root && newItem is BittensorSubnetListItem.Root -> true
            oldItem is BittensorSubnetListItem.Subnet && newItem is BittensorSubnetListItem.Subnet ->
                oldItem.row.metaId == newItem.row.metaId && oldItem.row.subnet.id == newItem.row.subnet.id
            else -> false
        }

        override fun areContentsTheSame(oldItem: BittensorSubnetListItem, newItem: BittensorSubnetListItem) = oldItem == newItem
    }

    private companion object {
        const val TYPE_ROOT = 3

        const val TYPE_SUBNET = 2
    }
}
