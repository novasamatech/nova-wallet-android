package io.novafoundation.nova.feature_account_impl.presentation.account.list.selecting

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountWithTotalBalance
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.SelectWalletRequester
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.SelectWalletResponder
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.AccountsAdapter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountSelectRules
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountListingMixin
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.list.WalletListViewModel
import io.novafoundation.nova.feature_account_impl.presentation.account.model.MetaAccountUi

class SelectWalletViewModel(
    accountListingMixinFactory: MetaAccountListingMixinFactory,
    private val router: AccountRouter,
    private val selectWalletResponder: SelectWalletResponder,
    private val accountInteractor: AccountInteractor,
    private val request: SelectWalletRequester.Request,
) : WalletListViewModel(), MetaAccountSelectRules {

    override val walletsListingMixin: MetaAccountListingMixin = accountListingMixinFactory.create(this, this)

    override val mode: AccountsAdapter.Mode = AccountsAdapter.Mode.SWITCH

    override fun accountClicked(accountModel: MetaAccountUi) {
        selectWalletResponder.respond(SelectWalletResponder.Response(accountModel.id))
        router.back()
    }

    override suspend fun select(metaAccountWithBalance: MetaAccountWithTotalBalance): Boolean {
        val chainAddress = accountInteractor.getChainAddress(metaAccountWithBalance.metaId, request.chainId)
        return chainAddress == request.chainAddress
    }
}
