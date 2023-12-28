package io.novafoundation.nova.feature_account_api.presenatation.account.listing

import androidx.annotation.ColorRes
import coil.ImageLoader
import io.novafoundation.nova.common.view.ChipLabelView
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountChipHolder
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountChipGroupRvItem

class AccountsAdapter(
    private val accountItemHandler: AccountHolder.AccountItemHandler,
    private val imageLoader: ImageLoader,
    @ColorRes private val chainBorderColor: Int,
    initialMode: AccountHolder.Mode
) : CommonAccountsAdapter<AccountChipGroupRvItem>(
    accountItemHandler = accountItemHandler,
    imageLoader = imageLoader,
    diffCallback = AccountDiffCallback(AccountChipGroupRvItem::class.java),
    groupFactory = { AccountChipHolder(ChipLabelView(it.context)) },
    groupBinder = { holder, item -> (holder as AccountChipHolder).bind(item) },
    chainBorderColor = chainBorderColor,
    initialMode = initialMode
)
