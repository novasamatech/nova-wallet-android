package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class LedgerMessageFormatterFactory(
    private val resourceManager: ResourceManager,
    private val migrationTracker: LedgerMigrationTracker,
    private val chainRegistry: ChainRegistry,
) {

    fun createLegacy(chainId: ChainId): LedgerMessageFormatter {
        return LegacyLedgerMessageFormatter(migrationTracker, resourceManager, chainRegistry, chainId)
    }

    fun createGeneric(): LedgerMessageFormatter {
        return GenericLedgerMessageFormatter(resourceManager)
    }
}
