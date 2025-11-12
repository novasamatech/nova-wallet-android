package io.novafoundation.nova.feature_account_impl.presentation.account.list.singleSelecting

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.mixin.common.SelectedAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountValidForTransactionListingMixinFactory
import io.novafoundation.nova.feature_account_api.presenatation.mixin.common.toMetaAccountsFilter
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectSingleWallet.SelectSingleWalletRequester
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectSingleWallet.SelectSingleWalletResponder
import kotlinx.coroutines.launch

class SelectSingleWalletViewModel(
    accountListingMixinFactory: MetaAccountValidForTransactionListingMixinFactory,
    private val router: AccountRouter,
    private val selectSingleWalletResponder: SelectSingleWalletResponder,
    private val request: SelectSingleWalletRequester.Request,
) : BaseViewModel() {

    val walletsListingMixin = accountListingMixinFactory.create(
        coroutineScope = this,
        chainId = request.chainId,
        selectedAccount = request.selectedMetaId?.let { SelectedAccountPayload.MetaAccount(it) },
        metaAccountFilter = request.filter.toMetaAccountsFilter()
    )

    fun accountClicked(accountModel: AccountUi) {
        launch {
            selectSingleWalletResponder.respond(SelectSingleWalletResponder.Response(accountModel.id))
            router.back()
        }
    }

    fun backClicked() {
        router.back()
    }
}
