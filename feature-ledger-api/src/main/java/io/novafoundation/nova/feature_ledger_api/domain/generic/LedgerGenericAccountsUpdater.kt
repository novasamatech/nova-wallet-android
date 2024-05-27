package io.novafoundation.nova.feature_ledger_api.domain.generic

import kotlinx.coroutines.flow.Flow

interface LedgerGenericAccountsUpdater {

    fun updateAvailableGenericAccounts(): Flow<Unit>
}
