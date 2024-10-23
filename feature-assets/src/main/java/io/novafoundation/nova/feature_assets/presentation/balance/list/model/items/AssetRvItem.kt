package io.novafoundation.nova.feature_assets.presentation.balance.list.model.items

import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableBaseItem
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableChildItem
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableParentItem
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel

interface BalanceListRvItem : ExpandableBaseItem {
    val itemId: String

    override fun getId(): String {
        return itemId
    }
}

interface AssetGroupRvItem : BalanceListRvItem

interface AssetRvItem : BalanceListRvItem {
    val asset: AssetModel
}
