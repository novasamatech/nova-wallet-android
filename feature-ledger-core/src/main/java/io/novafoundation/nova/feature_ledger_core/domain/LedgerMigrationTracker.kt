package io.novafoundation.nova.feature_ledger_core.domain

import io.novafoundation.nova.runtime.ext.isGenericLedgerAppSupported
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.findChains

interface LedgerMigrationTracker {

    suspend fun shouldUseMigrationApp(chainId: ChainId): Boolean
    
    suspend fun supportedChainsByGenericApp(): List<Chain>
}

internal class RealLedgerMigrationTracker(
    private val metadataShortenerService: MetadataShortenerService,
    private val chainRegistry: ChainRegistry
): LedgerMigrationTracker {

    override suspend fun shouldUseMigrationApp(chainId: ChainId): Boolean {
        return metadataShortenerService.isCheckMetadataHashAvailable(chainId)
    }

    override suspend fun supportedChainsByGenericApp(): List<Chain> {
       return chainRegistry.findChains {
           it.additional.isGenericLedgerAppSupported()
       }
    }
}
