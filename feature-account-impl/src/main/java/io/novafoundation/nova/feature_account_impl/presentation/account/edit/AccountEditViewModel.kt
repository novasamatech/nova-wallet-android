package io.novafoundation.nova.feature_account_impl.presentation.account.edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.mixin.api.AccountListingMixin
import io.novafoundation.nova.feature_account_impl.presentation.account.model.LightMetaAccountUi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class UnsyncedSwapPayload(val newState: List<LightMetaAccountUi>)

class EditAccountsViewModel(
    private val accountInteractor: AccountInteractor,
    private val accountRouter: AccountRouter,
    accountListingMixin: AccountListingMixin
) : BaseViewModel() {

    private val _deleteConfirmationLiveData = MutableLiveData<Event<Long>>()
    val deleteConfirmationLiveData: LiveData<Event<Long>> = _deleteConfirmationLiveData

    private val _unsyncedSwapLiveData = MutableLiveData<UnsyncedSwapPayload>()
    val unsyncedSwapLiveData: LiveData<UnsyncedSwapPayload> = _unsyncedSwapLiveData

    val accountListingLiveData = accountListingMixin.accountsFlow()
        .share()

    fun doneClicked() {
        accountRouter.back()
    }

    fun backClicked() {
        accountRouter.backToMainScreen()
    }

    fun deleteClicked(account: LightMetaAccountUi) = launch {
        if (!account.isSelected) {
            _deleteConfirmationLiveData.value = Event(account.id)
        }
    }

    fun deleteConfirmed(metaId: Long) {
        launch {
            accountInteractor.deleteAccount(metaId)
        }
    }

    fun onItemDrag(from: Int, to: Int) {
        launch {
            val currentState = _unsyncedSwapLiveData.value?.newState
                ?: accountListingLiveData.first()

            val newUnsyncedState = currentState.toMutableList()

            newUnsyncedState.add(to, newUnsyncedState.removeAt(from))

            _unsyncedSwapLiveData.value = UnsyncedSwapPayload(newUnsyncedState)
        }
    }

    fun onItemDrop() {
        val unsyncedState = _unsyncedSwapLiveData.value?.newState ?: return

        launch {
            val idsInNewOrder = unsyncedState.map(LightMetaAccountUi::id)

            accountInteractor.updateMetaAccountPositions(idsInNewOrder)
        }
    }

    fun addAccountClicked() {
        accountRouter.openAddAccount(AddAccountPayload.MetaAccount)
    }
}
