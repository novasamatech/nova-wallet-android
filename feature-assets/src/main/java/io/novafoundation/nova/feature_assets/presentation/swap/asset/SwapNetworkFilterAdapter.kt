package io.novafoundation.nova.feature_assets.presentation.swap.asset

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.feature_assets.databinding.ItemSwapNetworkFilterBinding

internal class SwapNetworkFilterAdapter(
    private val imageLoader: ImageLoader,
    private val onClick: (SwapNetworkFilter) -> Unit
) : BaseListAdapter<SwapNetworkFilter, SwapNetworkFilterAdapter.Holder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemSwapNetworkFilterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val model = getItem(position)
        with(holder.binder) {
            swapFilterNetworkIcon.setIcon(model.icon, imageLoader)
            swapFilterNetworkName.text = model.title
            swapFilterSelected.isChecked = model.selected
            root.setOnClickListener { onClick(model) }
        }
    }

    class Holder(val binder: ItemSwapNetworkFilterBinding) : BaseViewHolder(binder.root)

    private object Diff : DiffUtil.ItemCallback<SwapNetworkFilter>() {
        override fun areItemsTheSame(oldItem: SwapNetworkFilter, newItem: SwapNetworkFilter) = oldItem.chainId == newItem.chainId
        override fun areContentsTheSame(oldItem: SwapNetworkFilter, newItem: SwapNetworkFilter) = oldItem == newItem
    }
}
