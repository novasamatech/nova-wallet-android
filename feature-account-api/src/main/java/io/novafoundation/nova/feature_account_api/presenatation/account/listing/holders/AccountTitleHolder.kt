package io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders

import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.feature_account_api.databinding.ItemDelegatedAccountGroupBinding

import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountTitleGroupRvItem

class AccountTitleHolder(private val binder: ItemDelegatedAccountGroupBinding) : GroupedListHolder(binder.root) {

    fun bind(item: AccountTitleGroupRvItem) {
        binder.delegatedAccountGroup.text = item.title
    }
}
