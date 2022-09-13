package io.novafoundation.nova.feature_account_impl.presentation.account.list.switching

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountsAdapter
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountWithBalanceListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.list.WalletListViewModel
import kotlinx.coroutines.launch

class SwitchWalletViewModel(
    private val accountInteractor: AccountInteractor,
    private val router: AccountRouter,
    accountListingMixinFactory: MetaAccountWithBalanceListingMixinFactory,
) : WalletListViewModel() {

    override val walletsListingMixin = accountListingMixinFactory.create(this)

    override val mode: AccountsAdapter.Mode = AccountsAdapter.Mode.SWITCH

    override fun accountClicked(accountModel: AccountUi) {
        launch {
            accountInteractor.selectMetaAccount(accountModel.id)

            router.back()
        }
    }

    fun settingsClicked() {
        router.openWallets()
    }
}
