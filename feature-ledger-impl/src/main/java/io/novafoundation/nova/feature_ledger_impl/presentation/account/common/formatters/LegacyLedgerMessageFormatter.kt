package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LegacyLedgerMessageFormatter(
    private val migrationTracker: LedgerMigrationTracker,
    private val resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry,
    private val chainId: ChainId
): LedgerMessageFormatter {

    private var cache: String? = null
    private val cacheMutex = Mutex()

    override suspend fun appName(): String {
        return cacheMutex.withLock {
            if (cache == null) {
                cache = computeAppName()
            }

            cache!!
        }
    }

    private suspend fun computeAppName(): String {
        return if (migrationTracker.shouldUseMigrationApp(chainId)) {
            resourceManager.getString(R.string.account_ledger_migration_app)
        } else {
            val chain = chainRegistry.getChain(chainId)
            chain.name
        }
    }
}
