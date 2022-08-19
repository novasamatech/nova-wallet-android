package io.novafoundation.nova.feature_ledger_api.di

import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService

interface LedgerFeatureApi {

    val discoveryService: LedgerDeviceDiscoveryService

    val substrateLedgerApplication: SubstrateLedgerApplication
}
