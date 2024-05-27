package io.novafoundation.nova.feature_ledger_api.di

import io.novafoundation.nova.feature_ledger_api.domain.generic.LedgerGenericAccountsUpdater

interface LedgerFeatureApi {

    val ledgerGenericAccountsUpdater: LedgerGenericAccountsUpdater
}
