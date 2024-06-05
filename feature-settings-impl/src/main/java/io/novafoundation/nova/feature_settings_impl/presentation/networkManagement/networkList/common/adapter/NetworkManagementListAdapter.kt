package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import coil.ImageLoader
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter.items.NetworkListNetworkRvItem
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter.items.NetworkListNetworkViewHolder
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter.items.NetworkListRvItem
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter.items.NetworkListViewHolder

private const val NETWORK_VIEW_TYPE = 0

class NetworkManagementListAdapter(
    private val imageLoader: ImageLoader,
    private val itemHandler: ItemHandler
) : ListAdapter<NetworkListRvItem, NetworkListViewHolder>(NetworkManagementListDiffCallback()) {

    interface ItemHandler {

        fun onNetworkClicked(chainId: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkListViewHolder {
        return when (viewType) {
            NETWORK_VIEW_TYPE -> NetworkListNetworkViewHolder(parent, imageLoader, itemHandler)
            else -> throw IllegalStateException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: NetworkListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is NetworkListNetworkRvItem -> NETWORK_VIEW_TYPE
            else -> throw IllegalStateException("Unknown view type")
        }
    }
}

class NetworkManagementListDiffCallback : DiffUtil.ItemCallback<NetworkListRvItem>() {

    override fun areItemsTheSame(oldItem: NetworkListRvItem, newItem: NetworkListRvItem): Boolean {
        return oldItem.id == newItem.id
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: NetworkListRvItem, newItem: NetworkListRvItem): Boolean {
        return oldItem == newItem
    }
}
