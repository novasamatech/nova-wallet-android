package io.novafoundation.nova.feature_account_impl.data.repository.addAccount

import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountRepository
import io.novafoundation.nova.feature_account_api.data.proxy.sync.ProxySyncService

abstract class BaseAddAccountRepository<T>(
    private val proxySyncService: ProxySyncService
) : AddAccountRepository<T> {

    final override suspend fun addAccount(payload: T): Long {
        val metaId = addAccountInternal(payload)
        proxySyncService.startSyncing()
        return metaId
    }

    protected abstract suspend fun addAccountInternal(payload: T): Long
}
