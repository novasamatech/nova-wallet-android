package io.novafoundation.nova.feature_account_impl.presentation.account.list.selectAddress

import io.novafoundation.nova.common.utils.EmptyFilter
import io.novafoundation.nova.common.utils.Filter
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressRequester.Request
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressResponder
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountValidForTransactionListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.list.WalletListViewModel
import io.novafoundation.nova.feature_wallet_api.domain.filter.MetaAccountFilter
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
        mapFilter(request.filter)
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

    private fun mapFilter(filter: Request.Filter): Filter<MetaAccount> {
        return when (filter) {
            Request.Filter.Empty -> EmptyFilter()
            is Request.Filter.ExcludeMetaIds -> MetaAccountFilter(MetaAccountFilter.Mode.EXCLUDE, filter.metaIds)
        }
    }
}
