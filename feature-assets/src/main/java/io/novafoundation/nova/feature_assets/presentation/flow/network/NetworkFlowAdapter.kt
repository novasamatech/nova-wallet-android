package io.novafoundation.nova.feature_assets.presentation.flow.network

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_assets.databinding.ItemNetworkFlowBinding
import io.novafoundation.nova.feature_assets.presentation.flow.network.model.NetworkFlowRvItem

class NetworkFlowAdapter(
    private val imageLoader: ImageLoader,
    private val itemHandler: ItemNetworkHandler,
) : ListAdapter<NetworkFlowRvItem, NetworkFlowViewHolder>(DiffCallback) {

    interface ItemNetworkHandler {
        fun networkClicked(network: NetworkFlowRvItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkFlowViewHolder {
        return NetworkFlowViewHolder(ItemNetworkFlowBinding.inflate(parent.inflater(), parent, false), imageLoader, itemHandler)
    }

    override fun onBindViewHolder(holder: NetworkFlowViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object DiffCallback : DiffUtil.ItemCallback<NetworkFlowRvItem>() {

    override fun areItemsTheSame(oldItem: NetworkFlowRvItem, newItem: NetworkFlowRvItem): Boolean {
        return oldItem.chainId == newItem.chainId
    }

    override fun areContentsTheSame(oldItem: NetworkFlowRvItem, newItem: NetworkFlowRvItem): Boolean {
        return oldItem == newItem
    }
}

class NetworkFlowViewHolder(
    private val binder: ItemNetworkFlowBinding,
    private val imageLoader: ImageLoader,
    private val itemHandler: NetworkFlowAdapter.ItemNetworkHandler,
) : GroupedListHolder(binder.root) {

    fun bind(item: NetworkFlowRvItem) = with(containerView) {
        binder.itemNetworkImage.loadChainIcon(item.icon, imageLoader)
        binder.itemNetworkName.text = item.networkName
        binder.itemNetworkBalance.text = item.balance.token
        binder.itemNetworkPriceAmount.text = item.balance.fiat

        setOnClickListener { itemHandler.networkClicked(item) }
    }
}
