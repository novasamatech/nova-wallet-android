package io.novafoundation.nova.feature_assets.presentation.balance.list.model.items

import androidx.annotation.ColorRes
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableParentItem
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

data class TokenGroupUi(
    override val itemId: String,
    val tokenIcon: String?,
    val rate: String,
    val recentRateChange: String,
    @ColorRes val rateChangeColorRes: Int,
    val tokenSymbol: String,
    val singleItemGroup: Boolean,
    val balance: AmountModel,
    val groupType: GroupType
) : AssetGroupRvItem, ExpandableParentItem {

    sealed interface GroupType {
        object Group : GroupType

        data class SingleItem(val asset: AssetModel) : GroupType
    }
}
