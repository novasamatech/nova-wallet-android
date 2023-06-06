package io.novafoundation.nova.feature_account_impl.presentation.mixin.selectWallet

import io.novafoundation.nova.common.navigation.requireLastInput
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountsAdapter
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletResponder
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountWithBalanceListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.list.WalletListViewModel

class SelectWalletViewModel(
    private val router: AccountRouter,
    private val responder: SelectWalletResponder,
    accountListingMixinFactory: MetaAccountWithBalanceListingMixinFactory,
) : WalletListViewModel() {

    private val currentSelectedId = responder.requireLastInput().currentMetaId

    override val walletsListingMixin = accountListingMixinFactory.create(
        coroutineScope = this,
        isMetaAccountSelected = { currentSelectedId == it.id }
    )

    override val mode: AccountsAdapter.Mode = AccountsAdapter.Mode.SWITCH

    override fun accountClicked(accountModel: AccountUi) {
        responder.respond(SelectWalletCommunicator.Response(accountModel.id))
        router.back()
    }
}
