package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setCompoundDrawableTint
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setShimmerShown
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.shape.getMaskedRipple
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.items.NetworkConnectionRvItem
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.items.NetworkNodeRvItem
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.items.NetworkNodesAddCustomRvItem

class ChainNetworkManagementNodesAdapter(
    private val itemHandler: ItemHandler
) : ListAdapter<NetworkConnectionRvItem, ViewHolder>(DiffCallback()) {

    interface ItemHandler {

        fun selectNode(item: NetworkNodeRvItem)

        fun editNode(item: NetworkNodeRvItem)

        fun addNewNode()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            R.layout.item_chan_network_management_node -> ChainNetworkManagementNodeViewHolder(parent.inflateChild(viewType), itemHandler)

            R.layout.item_chan_network_management_add_node_button -> ChainNetworkManagementAddNodeButtonViewHolder(
                parent.inflateChild(viewType),
                itemHandler
            )

            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is ChainNetworkManagementNodeViewHolder) {
            holder.bind(getItem(position) as NetworkNodeRvItem)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is NetworkNodeRvItem -> R.layout.item_chan_network_management_node
            is NetworkNodesAddCustomRvItem -> R.layout.item_chan_network_management_add_node_button
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }
}

class ChainNetworkManagementAddNodeButtonViewHolder(
    view: View,
    private val itemHandler: ChainNetworkManagementNodesAdapter.ItemHandler
) : ViewHolder(view) {

    init {
        itemView.background = itemView.context.getMaskedRipple(cornerSizeInDp = 0)
        itemView.setOnClickListener {
            itemHandler.addNewNode()
        }
    }
}

class ChainNetworkManagementNodeViewHolder(
    view: View,
    private val itemHandler: ChainNetworkManagementNodesAdapter.ItemHandler
) : ViewHolder(view) {

    init {
        itemView.background = itemView.context.getMaskedRipple(cornerSizeInDp = 0)
    }

    fun bind(item: NetworkNodeRvItem) {
        with(itemView) {
            if (item.isSelectable) {
                itemView.setOnClickListener { itemHandler.selectNode(item) }
            } else {
                itemView.setOnClickListener(null)
            }

            chainNodeRadioButton.isChecked = item.isSelected
            chainNodeRadioButton.isEnabled = item.isSelectable
            chainNodeName.text = item.name
            chainNodeName.setTextColorRes(item.nameColorRes)
            chainNodeSocketAddress.text = item.unformattedUrl
            chainNodeConnectionStatusShimmering.setShimmerShown(item.connectionState.showShimmering)
            chainNodeConnectionState.setText(item.connectionState.name)
            item.connectionState.chainStatusColor?.let { chainNodeConnectionState.setTextColor(it) }
            chainNodeConnectionState.setDrawableStart(item.connectionState.chainStatusIcon, paddingInDp = 6)
            chainNodeConnectionState.setCompoundDrawableTint(item.connectionState.chainStatusIconColor)

            chainNodeEditButton.isVisible = item.isEditable && !item.isDeletable
            chainNodeManageButton.isVisible = item.isEditable && item.isDeletable
            chainNodeEditButton.setOnClickListener { itemHandler.editNode(item) }
            chainNodeManageButton.setOnClickListener { itemHandler.editNode(item) }
        }
    }
}

private class DiffCallback : DiffUtil.ItemCallback<NetworkConnectionRvItem>() {

    override fun areItemsTheSame(oldItem: NetworkConnectionRvItem, newItem: NetworkConnectionRvItem): Boolean {
        return when (oldItem) {
            is NetworkNodeRvItem -> newItem is NetworkNodeRvItem && oldItem.id == newItem.id
            is NetworkNodesAddCustomRvItem -> newItem is NetworkNodesAddCustomRvItem
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: NetworkConnectionRvItem, newItem: NetworkConnectionRvItem): Boolean {
        return oldItem == newItem
    }
}
