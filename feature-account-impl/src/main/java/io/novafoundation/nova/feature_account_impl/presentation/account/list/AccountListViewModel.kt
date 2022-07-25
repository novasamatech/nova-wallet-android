package io.novafoundation.nova.feature_account_impl.presentation.account.list

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitAction
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingOrDenyingAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.AccountsAdapter.Mode
import io.novafoundation.nova.feature_account_impl.presentation.account.model.MetaAccountUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AccountListViewModel(
    private val accountInteractor: AccountInteractor,
    private val accountRouter: AccountRouter,
    private val resourceManager: ResourceManager,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val accountListingMixinFactory: MetaAccountListingMixinFactory,
) : BaseViewModel() {

    val walletsListingMixin = accountListingMixinFactory.create(this)

    val mode = MutableStateFlow(Mode.VIEW)

    val toolbarAction = mode.map {
        if (it == Mode.VIEW) {
            resourceManager.getString(R.string.common_edit)
        } else {
            resourceManager.getString(R.string.common_done)
        }
    }
        .shareInBackground()

    val confirmAccountDeletion = actionAwaitableMixinFactory.confirmingOrDenyingAction<Unit>()

    fun accountClicked(accountModel: MetaAccountUi) {
        accountRouter.openAccountDetails(accountModel.id)
    }

    fun editClicked() {
        val newMode = if (mode.value == Mode.VIEW) Mode.EDIT else Mode.VIEW

        mode.value = newMode
    }

    fun deleteClicked(account: MetaAccountUi) = launch {
        if (account.isSelected) return@launch

        val deleteConfirmed = confirmAccountDeletion.awaitAction()

        if (deleteConfirmed) {
            accountInteractor.deleteAccount(account.id)
        }
    }

    fun backClicked() {
        accountRouter.back()
    }

    fun addAccountClicked() {
        accountRouter.openAddAccount(AddAccountPayload.MetaAccount)
    }
}
