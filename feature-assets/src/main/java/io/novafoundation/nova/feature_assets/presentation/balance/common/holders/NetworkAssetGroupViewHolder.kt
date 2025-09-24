package io.novafoundation.nova.feature_assets.presentation.balance.common.holders

import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.presentation.masking.setMaskableText
import io.novafoundation.nova.feature_assets.databinding.ItemNetworkAssetGroupBinding
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.NetworkGroupUi

class NetworkAssetGroupViewHolder(
    private val binder: ItemNetworkAssetGroupBinding,
) : GroupedListHolder(binder.root) {

    fun bind(assetGroup: NetworkGroupUi) = with(binder) {
        itemAssetGroupChain.setChain(assetGroup.chainUi)
        itemAssetGroupBalance.setMaskableText(assetGroup.groupBalanceFiat)
    }
}
