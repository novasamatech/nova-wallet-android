package io.novafoundation.nova.feature_account_api.presenatation.account.listing.items

import io.novafoundation.nova.common.view.ChipLabelModel
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountGroupRvItem

data class AccountChipGroupRvItem(
    val chipLabelModel: ChipLabelModel
) : AccountGroupRvItem {
    override fun isItemTheSame(other: AccountGroupRvItem): Boolean {
        return other is AccountChipGroupRvItem && chipLabelModel.title == other.chipLabelModel.title
    }
}
