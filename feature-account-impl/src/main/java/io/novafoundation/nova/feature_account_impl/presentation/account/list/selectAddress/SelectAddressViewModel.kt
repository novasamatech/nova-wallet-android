package io.novafoundation.nova.feature_account_impl.presentation.account.list.selectAddress

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressForTransactionRequester
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressForTransactionResponder
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountValidForTransactionListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.list.WalletListViewModel
import kotlinx.coroutines.launch

class SelectAddressViewModel(
    accountListingMixinFactory: MetaAccountValidForTransactionListingMixinFactory,
    private val router: AccountRouter,
    private val selectAddressResponder: SelectAddressForTransactionResponder,
    private val accountInteractor: AccountInteractor,
    private val request: SelectAddressForTransactionRequester.Request,
) : WalletListViewModel() {

    override val walletsListingMixin = accountListingMixinFactory.create(this, request.fromChainId, request.destinationChainId, request.selectedAddress)

    override val mode: AccountHolder.Mode = AccountHolder.Mode.SWITCH

    override fun accountClicked(accountModel: AccountUi) {
        launch {
            val address = accountInteractor.getChainAddress(accountModel.id, request.destinationChainId)
            if (address != null) {
                selectAddressResponder.respond(SelectAddressForTransactionResponder.Response(address))
                router.back()
            }
        }
    }
}
