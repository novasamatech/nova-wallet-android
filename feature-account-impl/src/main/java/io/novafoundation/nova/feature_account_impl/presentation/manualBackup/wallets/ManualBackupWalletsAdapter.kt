package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.wallets

import coil.ImageLoader
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountDiffCallback
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountGroupRvItem
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.CommonAccountsAdapter
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder
import io.novafoundation.nova.feature_account_impl.R

class ManualBackupAccountsAdapter(
    private val imageLoader: ImageLoader,
    private val itemHandler: AccountHolder.AccountItemHandler
) : CommonAccountsAdapter<EmptyGroupRVItem>(
    accountItemHandler = itemHandler,
    imageLoader = imageLoader,
    diffCallback = AccountDiffCallback(EmptyGroupRVItem::class.java),
    groupFactory = { throw IllegalStateException("No groups in this adapter") },
    groupBinder = { _, _ -> },
    chainBorderColor = R.color.bottom_sheet_background,
    initialMode = AccountHolder.Mode.SELECT
)

class EmptyGroupRVItem : AccountGroupRvItem {
    override fun isItemTheSame(other: AccountGroupRvItem): Boolean {
        return false
    }
}
