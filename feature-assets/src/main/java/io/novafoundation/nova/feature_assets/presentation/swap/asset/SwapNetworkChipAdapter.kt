package io.novafoundation.nova.feature_assets.presentation.swap.asset

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.ItemSwapNetworkChipBinding

internal class SwapNetworkChipAdapter(
    private val imageLoader: ImageLoader,
    private val onClick: (SwapNetworkChip) -> Unit
) : BaseListAdapter<SwapNetworkChip, SwapNetworkChipAdapter.Holder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemSwapNetworkChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val model = getItem(position)
        with(holder.binder) {
            val selected = when (model) {
                is SwapNetworkChip.All -> model.selected
                is SwapNetworkChip.Network -> model.network.selected
                is SwapNetworkChip.More -> false
            }
            swapChipIcon.isVisible = model !is SwapNetworkChip.All
            swapChipIcon.imageTintList = null
            swapChipLabel.text = when (model) {
                is SwapNetworkChip.All -> model.title
                is SwapNetworkChip.Network -> model.network.title.also { swapChipIcon.setIcon(model.network.icon, imageLoader) }
                is SwapNetworkChip.More -> model.title.also {
                    swapChipIcon.setImageResource(R.drawable.ic_swap_all_networks)
                    swapChipIcon.setImageTintRes(R.color.icon_primary)
                }
            }
            swapChipPill.background = root.context.getRoundedCornerDrawable(
                if (selected) R.color.button_background_primary else R.color.chips_background,
                strokeColorRes = if (selected) R.color.button_background_primary else R.color.container_border,
                cornerSizeInDp = 18
            )
            root.isSelected = selected
            root.contentDescription = swapChipLabel.text
            root.setOnClickListener { onClick(model) }
        }
    }

    class Holder(val binder: ItemSwapNetworkChipBinding) : BaseViewHolder(binder.root)

    private object Diff : DiffUtil.ItemCallback<SwapNetworkChip>() {
        override fun areItemsTheSame(oldItem: SwapNetworkChip, newItem: SwapNetworkChip): Boolean = when {
            oldItem is SwapNetworkChip.Network && newItem is SwapNetworkChip.Network -> oldItem.network.chainId == newItem.network.chainId
            else -> oldItem::class == newItem::class
        }
        override fun areContentsTheSame(oldItem: SwapNetworkChip, newItem: SwapNetworkChip) = oldItem == newItem
    }
}
