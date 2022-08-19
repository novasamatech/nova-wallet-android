package io.novafoundation.nova.feature_account_impl.presentation.account.list.switching

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.AccountsAdapter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountWithBalanceListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.list.WalletListViewModel
import io.novafoundation.nova.feature_account_impl.presentation.account.model.MetaAccountUi
import kotlinx.coroutines.launch

class SwitchWalletViewModel(
    private val accountInteractor: AccountInteractor,
    private val router: AccountRouter,
    accountListingMixinFactory: MetaAccountWithBalanceListingMixinFactory,
) : WalletListViewModel() {

    override val walletsListingMixin = accountListingMixinFactory.create(this)

    override val mode: AccountsAdapter.Mode = AccountsAdapter.Mode.SWITCH

    override fun accountClicked(accountModel: MetaAccountUi) {
        launch {
            accountInteractor.selectMetaAccount(accountModel.id)

            router.back()
        }
    }

    fun settingsClicked() {
        router.openWallets()
    }
}
