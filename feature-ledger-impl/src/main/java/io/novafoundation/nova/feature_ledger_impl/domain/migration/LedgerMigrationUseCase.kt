package io.novafoundation.nova.feature_ledger_impl.domain.migration

import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.legacyApp.LegacySubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp.MigrationSubstrateLedgerApplication
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface LedgerMigrationUseCase  {

    suspend fun determineAppForLegacyAccount(chainId: ChainId): SubstrateLedgerApplication
}

class RealLedgerMigrationUseCase(
    private val ledgerMigrationTracker: LedgerMigrationTracker,
    private val migrationApp: MigrationSubstrateLedgerApplication,
    private val legacyApp: LegacySubstrateLedgerApplication
) : LedgerMigrationUseCase {

    override suspend fun determineAppForLegacyAccount(chainId: ChainId): SubstrateLedgerApplication {
        return if (ledgerMigrationTracker.shouldUseMigrationApp(chainId)) {
            migrationApp
        } else {
            legacyApp
        }
    }
}
