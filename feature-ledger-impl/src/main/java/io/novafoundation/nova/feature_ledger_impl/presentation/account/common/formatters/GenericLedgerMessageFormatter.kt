package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_ledger_impl.R

class GenericLedgerMessageFormatter(
    private val resourceManager: ResourceManager
): LedgerMessageFormatter {

    override suspend fun appName(): String {
        return resourceManager.getString(R.string.account_ledger_migration_generic)
    }
}
