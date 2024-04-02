package io.novafoundation.nova.feature_account_impl.data.repository.addAccount

import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountRepository
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus.Event
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult

abstract class BaseAddAccountRepository<T>(
    private val proxySyncService: ProxySyncService,
    private val metaAccountChangesEventBus: MetaAccountChangesEventBus
) : AddAccountRepository<T> {

    final override suspend fun addAccount(payload: T): AddAccountResult {
        val addAccountResult = addAccountInternal(payload)
        proxySyncService.startSyncing()

        metaAccountChangesEventBus.notify(addAccountResult.toEvent())

        return addAccountResult
    }

    protected abstract suspend fun addAccountInternal(payload: T): AddAccountResult

    private fun AddAccountResult.toEvent(): Event {
        return when (this) {
            is AddAccountResult.AccountAdded -> Event.AccountAdded(metaId)
            is AddAccountResult.AccountChanged -> Event.AccountChanged(metaId)
        }
    }
}
