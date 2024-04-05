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

    override suspend fun addAccounts(payloads: List<T>): List<AddAccountResult> {
        val addAccountResults = payloads.map { addAccountInternal(it) }

        proxySyncService.startSyncing()

        addAccountResults.toEvents().forEach { metaAccountChangesEventBus.notify(it) }

        return addAccountResults
    }

    protected abstract suspend fun addAccountInternal(payload: T): AddAccountResult

    private fun AddAccountResult.toEvent(): Event {
        return when (this) {
            is AddAccountResult.AccountAdded -> Event.AccountAdded(listOf(metaId))
            is AddAccountResult.AccountChanged -> Event.AccountChanged(listOf(metaId))
        }
    }

    private fun List<AddAccountResult>.toEvents(): List<Event> {
        val addedAccounts = mutableListOf<Long>()
        val changedAccounts = mutableListOf<Long>()
        forEach {
            when (it) {
                is AddAccountResult.AccountAdded -> addedAccounts.add(it.metaId)
                is AddAccountResult.AccountChanged -> changedAccounts.add(it.metaId)
            }
        }

        return mutableListOf<Event>().apply {
            if (addedAccounts.isNotEmpty()) add(Event.AccountAdded(addedAccounts))
            if (changedAccounts.isNotEmpty()) add(Event.AccountChanged(changedAccounts))
        }
    }
}
