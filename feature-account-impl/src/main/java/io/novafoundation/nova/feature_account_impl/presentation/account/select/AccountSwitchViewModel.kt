package io.novafoundation.nova.feature_account_impl.presentation.account.select

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.AccountsAdapter.Mode
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.model.MetaAccountUi
import kotlinx.coroutines.launch

class AccountSwitchViewModel(
    private val accountInteractor: AccountInteractor,
    private val router: AccountRouter,
    accountListingMixinFactory: MetaAccountListingMixinFactory,
) : BaseViewModel() {

    val walletsListingMixin = accountListingMixinFactory.create(this)

    val mode = Mode.SWITCH

    fun accountClicked(accountModel: MetaAccountUi) = launch {
        accountInteractor.selectMetaAccount(accountModel.id)

        router.back()
    }

    fun settingsClicked() {
        router.openWallets()
    }
}
