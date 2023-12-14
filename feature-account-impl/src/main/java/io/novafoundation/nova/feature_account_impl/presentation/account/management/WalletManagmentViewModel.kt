package io.novafoundation.nova.feature_account_impl.presentation.account.management

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitAction
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingOrDenyingAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.holders.AccountHolder.Mode
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountWithBalanceListingMixinFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class WalletManagmentViewModel(
    private val accountInteractor: AccountInteractor,
    private val accountRouter: AccountRouter,
    private val resourceManager: ResourceManager,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val accountListingMixinFactory: MetaAccountWithBalanceListingMixinFactory
) : BaseViewModel() {

    val walletsListingMixin = accountListingMixinFactory.create(this)

    val mode = MutableStateFlow(Mode.SELECT)

    val toolbarAction = mode.map {
        if (it == Mode.SELECT) {
            resourceManager.getString(R.string.common_edit)
        } else {
            resourceManager.getString(R.string.common_done)
        }
    }
        .shareInBackground()

    val confirmAccountDeletion = actionAwaitableMixinFactory.confirmingOrDenyingAction<Unit>()

    fun accountClicked(accountModel: AccountUi) {
        accountRouter.openAccountDetails(accountModel.id)
    }

    fun editClicked() {
        val newMode = if (mode.value == Mode.SELECT) Mode.EDIT else Mode.SELECT

        mode.value = newMode
    }

    fun deleteClicked(account: AccountUi) = launch {
        val deleteConfirmed = confirmAccountDeletion.awaitAction()

        if (deleteConfirmed) {
            val isAllMetaAccountsWasDeleted = accountInteractor.deleteAccount(account.id)
            if (isAllMetaAccountsWasDeleted) {
                accountRouter.openWelcomeScreen()
            }
        }
    }

    fun backClicked() {
        accountRouter.back()
    }

    fun addAccountClicked() {
        accountRouter.openAddAccount(AddAccountPayload.MetaAccount)
    }
}
