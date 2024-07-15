package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters

import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class LedgerMessageFormatterFactory(
    private val resourceManager: ResourceManager,
    private val migrationTracker: LedgerMigrationTracker,
    private val chainRegistry: ChainRegistry,
    private val appLinksProvider: AppLinksProvider,
) {

    fun createLegacy(chainId: ChainId, showAlerts: Boolean): LedgerMessageFormatter {
        return LegacyLedgerMessageFormatter(migrationTracker, resourceManager, chainRegistry, appLinksProvider, chainId, showAlerts)
    }

    fun createGeneric(): LedgerMessageFormatter {
        return GenericLedgerMessageFormatter(resourceManager)
    }
}
