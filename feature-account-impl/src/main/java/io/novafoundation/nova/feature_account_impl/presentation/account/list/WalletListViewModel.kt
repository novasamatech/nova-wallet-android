package io.novafoundation.nova.feature_account_impl.presentation.account.list

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder.Mode
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountListingMixin
import kotlinx.coroutines.flow.MutableStateFlow

abstract class WalletListViewModel : BaseViewModel() {

    abstract val walletsListingMixin: MetaAccountListingMixin

    abstract val mode: Mode

    val searchQueryFlow = MutableStateFlow("")

    fun onSearchQueryChanged(query: String) {
        searchQueryFlow.value = query
    }

    abstract fun accountClicked(accountModel: AccountUi)
}
