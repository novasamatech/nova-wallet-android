package io.novafoundation.nova.feature_account_api.presenatation.account.listing.items

import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountGroupRvItem

data class AccountTitleGroupRvItem(
    val title: String
) : AccountGroupRvItem {
    override fun isItemTheSame(other: AccountGroupRvItem): Boolean {
        return other is AccountTitleGroupRvItem && title == other.title
    }
}
