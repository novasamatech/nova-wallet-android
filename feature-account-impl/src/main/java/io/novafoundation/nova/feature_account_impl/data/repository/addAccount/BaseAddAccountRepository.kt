package io.novafoundation.nova.feature_account_impl.data.repository.addAccount

import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.toAccountBusEvent

abstract class BaseAddAccountRepository<T>(
    private val metaAccountChangesEventBus: MetaAccountChangesEventBus
) : AddAccountRepository<T> {

    final override suspend fun addAccount(payload: T): AddAccountResult {
        val addAccountResult = addAccountInternal(payload)

        addAccountResult.toAccountBusEvent()?.let { metaAccountChangesEventBus.notify(it, source = null) }

        return addAccountResult
    }

    protected abstract suspend fun addAccountInternal(payload: T): AddAccountResult
}
