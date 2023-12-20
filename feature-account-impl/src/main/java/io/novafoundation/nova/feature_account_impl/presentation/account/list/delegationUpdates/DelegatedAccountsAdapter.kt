package io.novafoundation.nova.feature_account_impl.presentation.account.list.delegationUpdates

import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountDiffCallback
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountGroupViewHolderFactory
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.CommonAccountsAdapter
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountTitleHolder
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountTitleGroupRvItem
import io.novafoundation.nova.feature_account_impl.R

class DelegatedAccountsAdapter(
    private val imageLoader: ImageLoader
) : CommonAccountsAdapter<AccountTitleGroupRvItem>(
    accountItemHandler = null,
    imageLoader = imageLoader,
    diffCallback = AccountDiffCallback(AccountTitleGroupRvItem::class.java),
    groupFactory = DelegatedAccountsGroupFactory(),
    groupBinder = { holder, item -> (holder as AccountTitleHolder).bind(item) },
    chainBorderColor = R.color.bottom_sheet_background,
    initialMode = AccountHolder.Mode.VIEW
)

private class DelegatedAccountsGroupFactory : AccountGroupViewHolderFactory {

    override fun create(parent: ViewGroup): GroupedListHolder {
        return AccountTitleHolder(
            parent.inflateChild(
                R.layout.item_delegated_account_group,
                false
            )
        )
    }
}
