package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.transform.CircleCropTransformation
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.feature_staking_impl.databinding.ItemBittensorPositionBinding

class BittensorPositionAdapter(
    private val imageLoader: ImageLoader,
    private val onClick: (BittensorPositionUi) -> Unit
) : ListAdapter<BittensorPositionUi, BittensorPositionAdapter.Holder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(ItemBittensorPositionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(private val binding: ItemBittensorPositionBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BittensorPositionUi) = with(binding) {
            positionIcon.updateLayoutParams {
                width = item.iconSizeDp.dp(root.context)
                height = item.iconSizeDp.dp(root.context)
            }
            positionIcon.setIcon(item.icon, imageLoader) {
                transformations(CircleCropTransformation())
                error(io.novafoundation.nova.feature_staking_impl.R.drawable.ic_fallback_network_icon)
            }
            positionTitle.text = item.title
            positionSubtitle.text = item.subtitle
            positionAmount.text = item.amount.token
            positionFiat.text = item.amount.fiat
            positionFiat.isVisible = item.amount.fiat != null
            val clickable = item.action != BittensorPositionAction.None
            positionChevron.isVisible = clickable
            root.setOnClickListener(if (clickable) android.view.View.OnClickListener { onClick(item) } else null)
        }
    }

    private object Diff : DiffUtil.ItemCallback<BittensorPositionUi>() {

        override fun areItemsTheSame(oldItem: BittensorPositionUi, newItem: BittensorPositionUi) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: BittensorPositionUi, newItem: BittensorPositionUi) = oldItem == newItem
    }
}
