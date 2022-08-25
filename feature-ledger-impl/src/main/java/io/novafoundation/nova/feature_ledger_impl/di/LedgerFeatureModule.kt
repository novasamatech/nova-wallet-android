package io.novafoundation.nova.feature_ledger_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_api.sdk.transport.LedgerTransport
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.RealSubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_impl.sdk.connection.ble.LedgerBleManager
import io.novafoundation.nova.feature_ledger_impl.sdk.discovery.ble.BleLedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.sdk.transport.ChunkedLedgerTransport

@Module
class LedgerFeatureModule {

    @Provides
    @FeatureScope
    fun provideLedgerTransport(): LedgerTransport = ChunkedLedgerTransport()

    @Provides
    @FeatureScope
    fun provideSubstrateLedgerApplication(
        transport: LedgerTransport
    ): SubstrateLedgerApplication = RealSubstrateLedgerApplication(transport)

    @Provides
    @FeatureScope
    fun provideLedgerBleManager(
        contextManager: ContextManager
    ) = LedgerBleManager(contextManager)

    @Provides
    @FeatureScope
    fun provideLedgerDeviceDiscoveryService(
        bluetoothManager: BluetoothManager,
        ledgerBleManager: LedgerBleManager
    ): LedgerDeviceDiscoveryService = BleLedgerDeviceDiscoveryService(
        bluetoothManager = bluetoothManager,
        ledgerBleManager = ledgerBleManager
    )
}
