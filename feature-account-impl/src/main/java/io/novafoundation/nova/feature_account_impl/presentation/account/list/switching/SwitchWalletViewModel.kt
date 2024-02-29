package io.novafoundation.nova.feature_account_impl.presentation.account.list.switching

import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.feature_account_api.data.proxy.MetaAccountsUpdatesRegistry
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder
import io.novafoundation.nova.feature_account_api.presenatation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountWithBalanceListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.list.WalletListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SwitchWalletViewModel(
    private val accountInteractor: AccountInteractor,
    private val router: AccountRouter,
    private val metaAccountsUpdatesRegistry: MetaAccountsUpdatesRegistry,
    private val rootScope: RootScope,
    accountListingMixinFactory: MetaAccountWithBalanceListingMixinFactory,
) : WalletListViewModel() {

    override val walletsListingMixin = accountListingMixinFactory.create(this)

    override val mode: AccountHolder.Mode = AccountHolder.Mode.SWITCH

    init {
        if (metaAccountsUpdatesRegistry.hasUpdates()) {
            router.openDelegatedAccountsUpdates()
        }
    }

    override fun accountClicked(accountModel: AccountUi) {
        launch {
            accountInteractor.selectMetaAccount(accountModel.id)

            router.back()
        }
    }

    fun settingsClicked() {
        router.openWallets()
    }

    fun onDestroy() {
        rootScope.launch(Dispatchers.Default) {
            metaAccountsUpdatesRegistry.clear()
            accountInteractor.removeDeactivatedMetaAccounts()
        }
    }
}
