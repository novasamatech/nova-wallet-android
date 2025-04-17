package io.novafoundation.nova.feature_account_impl.presentation.node.list

import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_account_impl.databinding.ItemNodeBinding
import io.novafoundation.nova.feature_account_impl.databinding.ItemNodeGroupBinding
import io.novafoundation.nova.feature_account_impl.presentation.node.model.NodeHeaderModel
import io.novafoundation.nova.feature_account_impl.presentation.node.model.NodeModel

class NodesAdapter(
    private val nodeItemHandler: NodeItemHandler
) : GroupedListAdapter<NodeHeaderModel, NodeModel>(NodesDiffCallback) {

    interface NodeItemHandler {

        fun infoClicked(nodeModel: NodeModel)

        fun checkClicked(nodeModel: NodeModel)

        fun deleteClicked(nodeModel: NodeModel)
    }

    private var editMode = false

    fun switchToEdit(editable: Boolean) {
        editMode = editable

        val firstCustomNodeIndex = currentList.indexOfFirst { it is NodeModel && !it.isDefault }

        if (firstCustomNodeIndex == -1) return

        val customNodesCount = currentList.size - firstCustomNodeIndex
        notifyItemRangeChanged(firstCustomNodeIndex, customNodesCount)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return NodeGroupHolder(ItemNodeGroupBinding.inflate(parent.inflater(), parent, false))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return NodeHolder(ItemNodeBinding.inflate(parent.inflater(), parent, false))
    }

    override fun bindGroup(holder: GroupedListHolder, group: NodeHeaderModel) {
        (holder as NodeGroupHolder).bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: NodeModel) {
        (holder as NodeHolder).bind(child, nodeItemHandler, editMode)
    }
}

class NodeGroupHolder(private val binder: ItemNodeGroupBinding) : GroupedListHolder(binder.root) {
    fun bind(nodeHeaderModel: NodeHeaderModel) {
        binder.nodeGroupTitle.text = nodeHeaderModel.title
    }
}

class NodeHolder(private val binder: ItemNodeBinding) : GroupedListHolder(binder.root) {

    fun bind(
        nodeModel: NodeModel,
        handler: NodesAdapter.NodeItemHandler,
        editMode: Boolean
    ) {
        with(containerView) {
            binder.nodeTitle.text = nodeModel.name
            binder.nodeHost.text = nodeModel.link

            val isChecked = nodeModel.isActive

            binder.nodeCheck.visibility = if (isChecked) View.VISIBLE else View.INVISIBLE

            if (!isChecked && !nodeModel.isDefault && editMode) {
                binder.nodeDelete.visibility = View.VISIBLE
                binder.nodeDelete.setOnClickListener { handler.deleteClicked(nodeModel) }
                binder.nodeInfo.visibility = View.INVISIBLE
                binder.nodeInfo.setOnClickListener(null)
                isEnabled = false
                setOnClickListener(null)
            } else {
                binder.nodeDelete.visibility = View.GONE
                binder.nodeDelete.setOnClickListener(null)
                binder.nodeInfo.visibility = View.VISIBLE
                binder.nodeInfo.setOnClickListener { handler.infoClicked(nodeModel) }
                isEnabled = true
                setOnClickListener { handler.checkClicked(nodeModel) }
            }

            binder.nodeIcon.setImageResource(nodeModel.networkModelType.icon)
        }
    }
}

private object NodesDiffCallback : BaseGroupedDiffCallback<NodeHeaderModel, NodeModel>(NodeHeaderModel::class.java) {

    override fun areGroupItemsTheSame(oldItem: NodeHeaderModel, newItem: NodeHeaderModel): Boolean {
        return oldItem.title == newItem.title
    }

    override fun areGroupContentsTheSame(oldItem: NodeHeaderModel, newItem: NodeHeaderModel): Boolean {
        return oldItem == newItem
    }

    override fun areChildItemsTheSame(oldItem: NodeModel, newItem: NodeModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areChildContentsTheSame(oldItem: NodeModel, newItem: NodeModel): Boolean {
        return oldItem == newItem
    }
}
