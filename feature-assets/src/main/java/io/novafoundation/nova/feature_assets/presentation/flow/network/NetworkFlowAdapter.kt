package io.novafoundation.nova.feature_assets.presentation.flow.network

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.flow.network.model.NetworkFlowRvItem
import kotlinx.android.synthetic.main.item_network_flow.view.itemNetworkBalance
import kotlinx.android.synthetic.main.item_network_flow.view.itemNetworkImage
import kotlinx.android.synthetic.main.item_network_flow.view.itemNetworkPriceAmount
import kotlinx.android.synthetic.main.item_network_flow.view.itemNetworkName

class NetworkFlowAdapter(
    private val imageLoader: ImageLoader,
    private val itemHandler: ItemNetworkHandler,
) : ListAdapter<NetworkFlowRvItem, NetworkFlowViewHolder>(DiffCallback) {

    interface ItemNetworkHandler {
        fun networkClicked(network: NetworkFlowRvItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkFlowViewHolder {
        return NetworkFlowViewHolder(parent.inflateChild(R.layout.item_network_flow), imageLoader, itemHandler)
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
    containerView: View,
    private val imageLoader: ImageLoader,
    private val itemHandler: NetworkFlowAdapter.ItemNetworkHandler,
) : GroupedListHolder(containerView) {

    fun bind(item: NetworkFlowRvItem) = with(containerView) {
        itemNetworkImage.loadChainIcon(item.icon, imageLoader)
        itemNetworkName.text = item.networkName
        itemNetworkBalance.text = item.balance.token
        itemNetworkPriceAmount.text = item.balance.fiat

        setOnClickListener { itemHandler.networkClicked(item) }
    }
}
