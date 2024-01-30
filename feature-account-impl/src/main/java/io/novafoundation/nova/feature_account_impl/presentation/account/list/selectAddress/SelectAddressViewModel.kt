package io.novafoundation.nova.feature_account_impl.presentation.account.list.selectAddress

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressRequester.Request
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressResponder
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountValidForTransactionListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.list.WalletListViewModel
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.toMetaAccountsFilter
import kotlinx.coroutines.launch

class SelectAddressViewModel(
    accountListingMixinFactory: MetaAccountValidForTransactionListingMixinFactory,
    private val router: AccountRouter,
    private val selectAddressResponder: SelectAddressResponder,
    private val accountInteractor: AccountInteractor,
    private val request: Request,
) : WalletListViewModel() {

    override val walletsListingMixin = accountListingMixinFactory.create(
        this,
        request.chainId,
        request.selectedAddress,
        request.filter.toMetaAccountsFilter()
    )

    override val mode: AccountHolder.Mode = AccountHolder.Mode.SWITCH

    override fun accountClicked(accountModel: AccountUi) {
        launch {
            val address = accountInteractor.getChainAddress(accountModel.id, request.chainId)
            if (address != null) {
                selectAddressResponder.respond(SelectAddressResponder.Response(address))
                router.back()
            }
        }
    }
}
