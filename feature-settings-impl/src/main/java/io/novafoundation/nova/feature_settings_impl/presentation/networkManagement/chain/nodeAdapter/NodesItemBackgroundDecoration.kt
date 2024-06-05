package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.decoration.BackgroundItemDecoration
import io.novafoundation.nova.common.list.decoration.DividerItemDecoration
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.nodeAdapter.items.NetworkConnectionRvItem

class NodesItemBackgroundDecoration(context: Context) : BackgroundItemDecoration(
    context = context,
    outerHorizontalMarginDp = 16,
    innerVerticalPaddingDp = 0
) {

    override fun shouldApplyDecoration(holder: RecyclerView.ViewHolder): Boolean {
        return holder.isNodeItem()
    }
}

class NodesItemDividerDecoration(context: Context) : DividerItemDecoration(context, dividerMarginDp = 32) {

    override fun shouldApplyDecorationBetween(top: RecyclerView.ViewHolder, bottom: RecyclerView.ViewHolder): Boolean {
        return top.isNodeItem() && bottom.isNodeItem()
    }
}

private fun RecyclerView.ViewHolder.isNodeItem(): Boolean {
    return this is ChainNetworkManagementNodeViewHolder || this is ChainNetworkManagementAddNodeButtonViewHolder
}
