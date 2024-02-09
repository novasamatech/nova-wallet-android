package io.novafoundation.nova.feature_account_impl.presentation.mixin.selectWallet

import io.novafoundation.nova.common.navigation.requireLastInput
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder
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

    override val mode: AccountHolder.Mode = AccountHolder.Mode.SWITCH

    override fun accountClicked(accountModel: AccountUi) {
        responder.respond(SelectWalletCommunicator.Response(accountModel.id))
        router.back()
    }
}
