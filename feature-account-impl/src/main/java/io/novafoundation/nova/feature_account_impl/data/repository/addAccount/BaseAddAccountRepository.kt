package io.novafoundation.nova.feature_account_impl.data.repository.addAccount

import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus.Event
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult

abstract class BaseAddAccountRepository<T>(
    private val metaAccountChangesEventBus: MetaAccountChangesEventBus
) : AddAccountRepository<T> {

    final override suspend fun addAccount(payload: T): AddAccountResult {
        val addAccountResult = addAccountInternal(payload)

        addAccountResult.toEvent()?.let { metaAccountChangesEventBus.notify(it, source = null) }

        return addAccountResult
    }

    protected abstract suspend fun addAccountInternal(payload: T): AddAccountResult

    private fun AddAccountResult.toEvent(): Event? {
        return when (this) {
            is AddAccountResult.HadEffect -> toEvent()
            is AddAccountResult.NoOp -> null
        }
    }

    private fun AddAccountResult.HadEffect.toEvent(): Event {
        return when(this) {
            is AddAccountResult.AccountAdded -> Event.AccountAdded(metaId, type)
            is AddAccountResult.AccountChanged -> Event.AccountStructureChanged(metaId, type)
            is AddAccountResult.Batch -> Event.BatchUpdate(updates.map { it.toEvent() })
        }
    }
}
