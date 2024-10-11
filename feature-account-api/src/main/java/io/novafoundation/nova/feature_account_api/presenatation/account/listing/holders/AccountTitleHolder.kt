package io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders

import android.view.View
import io.novafoundation.nova.common.list.GroupedListHolder

import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountTitleGroupRvItem

class AccountTitleHolder(override val containerView: View) : GroupedListHolder(containerView) {

    fun bind(item: AccountTitleGroupRvItem) {
        containerView.delegatedAccountGroup.text = item.title
    }
}
