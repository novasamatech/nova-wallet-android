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
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.getMaskedRipple
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.items.NetworkConnectionRvItem
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.items.NetworkNodeRvItem
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.items.NetworkNodesAddCustomRvItem
import kotlinx.android.synthetic.main.item_chan_network_management_node.view.chainNodeConnectionState
import kotlinx.android.synthetic.main.item_chan_network_management_node.view.chainNodeConnectionStatusShimmering
import kotlinx.android.synthetic.main.item_chan_network_management_node.view.chainNodeEditButton
import kotlinx.android.synthetic.main.item_chan_network_management_node.view.chainNodeName
import kotlinx.android.synthetic.main.item_chan_network_management_node.view.chainNodeRadioButton
import kotlinx.android.synthetic.main.item_chan_network_management_node.view.chainNodeSocketAddress

private const val VIEW_TYPE_NODE = 0
private const val VIEW_TYPE_ADD_CUSTOM = 1

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
            VIEW_TYPE_NODE -> ChainNetworkManagementNodeViewHolder(parent.inflateChild(R.layout.item_chan_network_management_node), itemHandler)

            VIEW_TYPE_ADD_CUSTOM -> ChainNetworkManagementAddNodeButtonViewHolder(
                parent.inflateChild(R.layout.item_chan_network_management_add_node_button),
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
            is NetworkNodeRvItem -> VIEW_TYPE_NODE
            is NetworkNodesAddCustomRvItem -> VIEW_TYPE_ADD_CUSTOM
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
            itemView.setOnClickListener { itemHandler.selectNode(item) }
            chainNodeRadioButton.isChecked = item.isSelected
            chainNodeName.text = item.name
            chainNodeSocketAddress.text = item.socketAddress
            chainNodeConnectionStatusShimmering.setShimmerShown(item.connectionState.showShimmering)
            chainNodeConnectionState.setTextOrHide(item.connectionState.name)
            chainNodeConnectionState.setTextColor(item.connectionState.chainStatusColor)
            chainNodeConnectionState.setDrawableStart(item.connectionState.chainStatusIcon, paddingInDp = 6)
            chainNodeConnectionState.setCompoundDrawableTint(item.connectionState.chainStatusIconColor)

            chainNodeEditButton.isVisible = item.isEditable
            chainNodeEditButton.setOnClickListener {
                itemHandler.editNode(item)
            }
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
