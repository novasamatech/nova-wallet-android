package io.novafoundation.nova.feature_assets.presentation.balance.list.model.items

import io.novafoundation.nova.feature_assets.presentation.model.AssetModel

interface BalanceListRvItem {
    val id: String
}

interface AssetGroupRvItem : BalanceListRvItem

interface AssetRvItem : BalanceListRvItem {
    val asset: AssetModel
}
