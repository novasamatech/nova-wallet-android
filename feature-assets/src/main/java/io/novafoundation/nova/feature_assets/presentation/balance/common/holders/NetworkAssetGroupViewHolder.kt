package io.novafoundation.nova.feature_assets.presentation.balance.common.holders

import android.view.View
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableViewHolder
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.NetworkGroupUi
import kotlinx.android.synthetic.main.item_network_asset_group.view.itemAssetGroupBalance
import kotlinx.android.synthetic.main.item_network_asset_group.view.itemAssetGroupChain

class NetworkAssetGroupViewHolder(
    containerView: View,
) : GroupedListHolder(containerView), ExpandableViewHolder {

    fun bind(assetGroup: NetworkGroupUi) = with(containerView) {
        itemAssetGroupChain.setChain(assetGroup.chainUi)
        itemAssetGroupBalance.text = assetGroup.groupBalanceFiat
    }
}
