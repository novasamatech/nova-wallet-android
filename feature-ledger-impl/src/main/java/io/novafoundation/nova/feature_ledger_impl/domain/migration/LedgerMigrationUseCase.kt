package io.novafoundation.nova.feature_ledger_impl.domain.migration

import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.legacyApp.LegacySubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp.GenericSubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp.MigrationSubstrateLedgerApplication
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface LedgerMigrationUseCase {

    suspend fun determineLedgerApp(chainId: ChainId, ledgerVariant: LedgerVariant): SubstrateLedgerApplication
}

suspend fun LedgerMigrationUseCase.determineAppForLegacyAccount(chainId: ChainId): SubstrateLedgerApplication {
    return determineLedgerApp(chainId, LedgerVariant.LEGACY)
}

class RealLedgerMigrationUseCase(
    private val ledgerMigrationTracker: LedgerMigrationTracker,
    private val migrationApp: MigrationSubstrateLedgerApplication,
    private val legacyApp: LegacySubstrateLedgerApplication,
    private val genericApp: GenericSubstrateLedgerApplication,
) : LedgerMigrationUseCase {

    override suspend fun determineLedgerApp(chainId: ChainId, ledgerVariant: LedgerVariant): SubstrateLedgerApplication {
        return when {
            ledgerVariant == LedgerVariant.GENERIC -> genericApp
            ledgerMigrationTracker.shouldUseMigrationApp(chainId) -> migrationApp
            else -> legacyApp
        }
    }
}
