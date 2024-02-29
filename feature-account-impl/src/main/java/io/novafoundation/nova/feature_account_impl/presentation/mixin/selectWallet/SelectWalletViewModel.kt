package io.novafoundation.nova.feature_account_impl.presentation.mixin.selectWallet

import io.novafoundation.nova.common.navigation.requireLastInput
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletResponder
import io.novafoundation.nova.feature_account_api.presenatation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountWithBalanceListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.SelectedMetaAccountState
import io.novafoundation.nova.feature_account_impl.presentation.account.list.WalletListViewModel

class SelectWalletViewModel(
    private val router: AccountRouter,
    private val responder: SelectWalletResponder,
    accountListingMixinFactory: MetaAccountWithBalanceListingMixinFactory,
) : WalletListViewModel() {

    private val currentSelectedIdFlow = flowOf {
        val selectedMetaId = responder.requireLastInput().currentMetaId
        SelectedMetaAccountState.Specified(setOf(selectedMetaId))
    }

    override val walletsListingMixin = accountListingMixinFactory.create(
        coroutineScope = this,
        metaAccountSelectedFlow = currentSelectedIdFlow
    )

    override val mode: AccountHolder.Mode = AccountHolder.Mode.SWITCH

    override fun accountClicked(accountModel: AccountUi) {
        responder.respond(SelectWalletCommunicator.Response(accountModel.id))
        router.back()
    }
}
