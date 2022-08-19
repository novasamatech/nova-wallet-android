package io.novafoundation.nova.feature_account_impl.presentation.account.list.selecting

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.SelectAddressRequester
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.SelectAddressResponder
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.AccountsAdapter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountWithChainAddressListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.list.WalletListViewModel
import io.novafoundation.nova.feature_account_impl.presentation.account.model.MetaAccountUi
import kotlinx.coroutines.launch

class SelectAddressViewModel(
    accountListingMixinFactory: MetaAccountWithChainAddressListingMixinFactory,
    private val router: AccountRouter,
    private val selectAddressResponder: SelectAddressResponder,
    private val accountInteractor: AccountInteractor,
    private val request: SelectAddressRequester.Request,
) : WalletListViewModel() {

    override val walletsListingMixin = accountListingMixinFactory.create(this, request.chainId, request.initialAddress)

    override val mode: AccountsAdapter.Mode = AccountsAdapter.Mode.SWITCH

    override fun accountClicked(accountModel: MetaAccountUi) {
        launch {
            val address = accountInteractor.getChainAddress(accountModel.id, request.chainId)
            if (address != null) {
                selectAddressResponder.respond(SelectAddressResponder.Response(address))
                router.back()
            }
        }
    }
}
