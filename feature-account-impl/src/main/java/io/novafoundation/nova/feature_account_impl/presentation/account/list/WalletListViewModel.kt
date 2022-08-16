package io.novafoundation.nova.feature_account_impl.presentation.account.list

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.AccountsAdapter.Mode
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountListingMixin
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.model.MetaAccountUi

abstract class WalletListViewModel : BaseViewModel() {

    abstract val walletsListingMixin: MetaAccountListingMixin

    abstract val mode: Mode

    abstract fun accountClicked(accountModel: MetaAccountUi)
}
