package io.novafoundation.nova.feature_ledger_core.di

import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker

interface LedgerCoreApi {

    val ledgerMigrationTracker: LedgerMigrationTracker
}
