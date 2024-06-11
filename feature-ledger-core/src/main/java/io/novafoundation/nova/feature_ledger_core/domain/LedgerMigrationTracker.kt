package io.novafoundation.nova.feature_ledger_core.domain

import io.novafoundation.nova.common.utils.mapNotNullToSet
import io.novafoundation.nova.runtime.ext.isGenericLedgerAppSupported
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.findChains
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface LedgerMigrationTracker {

    suspend fun shouldUseMigrationApp(chainId: ChainId): Boolean

    suspend fun supportedChainsByGenericApp(): List<Chain>

    suspend fun anyChainSupportsMigrationApp(): Boolean

    fun supportedChainIdsByGenericAppFlow(): Flow<Set<ChainId>>
}

internal class RealLedgerMigrationTracker(
    private val metadataShortenerService: MetadataShortenerService,
    private val chainRegistry: ChainRegistry
) : LedgerMigrationTracker {

    override suspend fun shouldUseMigrationApp(chainId: ChainId): Boolean {
        val supportedFromRuntime = metadataShortenerService.isCheckMetadataHashAvailable(chainId)

        // We additionally check for configuration flag since Kusama will upgrade before Generic/Migration apps will be released
        // We can lift this restriction once Generic/Migration apps are released
        val supportedFromLedger = chainRegistry.getChain(chainId).additional.isGenericLedgerAppSupported()

        return supportedFromRuntime && supportedFromLedger
    }

    override suspend fun supportedChainsByGenericApp(): List<Chain> {
        return chainRegistry.findChains {
            it.additional.isGenericLedgerAppSupported()
        }
    }

    override suspend fun anyChainSupportsMigrationApp(): Boolean {
        return supportedChainsByGenericApp().isNotEmpty()
    }

    override fun supportedChainIdsByGenericAppFlow(): Flow<Set<ChainId>> {
        return chainRegistry.currentChains.map { chains ->
            chains.mapNotNullToSet { chain ->
                chain.id.takeIf { chain.additional.isGenericLedgerAppSupported() }
            }
        }.distinctUntilChanged()
    }
}
