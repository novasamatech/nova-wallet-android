package io.novafoundation.nova.feature_account_impl.presentation.account.list

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountsAdapter.Mode
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountListingMixin

abstract class WalletListViewModel : BaseViewModel() {

    abstract val walletsListingMixin: MetaAccountListingMixin

    abstract val mode: Mode

    abstract fun accountClicked(accountModel: AccountUi)
}
