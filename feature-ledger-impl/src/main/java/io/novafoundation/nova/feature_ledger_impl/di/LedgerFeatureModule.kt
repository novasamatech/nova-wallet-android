package io.novafoundation.nova.feature_ledger_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.feature_ledger_api.data.repository.LedgerRepository
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_api.sdk.transport.LedgerTransport
import io.novafoundation.nova.feature_ledger_impl.data.repository.RealLedgerRepository
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.RealSelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.SelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessagePresentable
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.SingleSheetLedgerMessagePresentable
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.legacyApp.LegacySubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp.MigrationSubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp.metadata.MetadataShortenerService
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp.metadata.RealMetadataShortenerService
import io.novafoundation.nova.feature_ledger_impl.sdk.connection.ble.LedgerBleManager
import io.novafoundation.nova.feature_ledger_impl.sdk.discovery.CompoundLedgerDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.sdk.discovery.ble.BleLedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.sdk.discovery.usb.UsbLedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.sdk.transport.ChunkedLedgerTransport
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.rpc.RpcCalls

@Module
class LedgerFeatureModule {

    @Provides
    @FeatureScope
    fun provideLedgerTransport(): LedgerTransport = ChunkedLedgerTransport()

    @Provides
    @FeatureScope
    fun provideSubstrateLedgerApplication(
        transport: LedgerTransport,
        ledgerRepository: LedgerRepository,
    ) = LegacySubstrateLedgerApplication(transport, ledgerRepository)

    @Provides
    @FeatureScope
    fun provideMigrationLedgerApplication(
        transport: LedgerTransport,
        chainRegistry: ChainRegistry,
        ledgerRepository: LedgerRepository,
        metadataShortenerService: MetadataShortenerService
    ) = MigrationSubstrateLedgerApplication(
        transport = transport,
        chainRegistry = chainRegistry,
        metadataShortenerService = metadataShortenerService,
        ledgerRepository = ledgerRepository
    )

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
    ) = BleLedgerDeviceDiscoveryService(
        bluetoothManager = bluetoothManager,
        ledgerBleManager = ledgerBleManager
    )

    @Provides
    @FeatureScope
    fun provideUsbDeviceDiscoveryService(
       contextManager: ContextManager
    ) = UsbLedgerDeviceDiscoveryService(contextManager)

    @Provides
    @FeatureScope
    fun provideDeviceDiscoveryService(
        bleLedgerDeviceDiscoveryService: BleLedgerDeviceDiscoveryService,
        usbLedgerDeviceDiscoveryService: UsbLedgerDeviceDiscoveryService
    ): LedgerDeviceDiscoveryService = CompoundLedgerDiscoveryService(
        bleLedgerDeviceDiscoveryService,
        usbLedgerDeviceDiscoveryService
    )

    @Provides
    @FeatureScope
    fun provideRepository(
        secretStoreV2: SecretStoreV2
    ): LedgerRepository = RealLedgerRepository(secretStoreV2)

    @Provides
    fun provideLedgerMessagePresentable(): LedgerMessagePresentable = SingleSheetLedgerMessagePresentable()

    @Provides
    @FeatureScope
    fun provideSelectAddressInteractor(
        ledgerApplication: LegacySubstrateLedgerApplication,
        migrationApplication: MigrationSubstrateLedgerApplication,
        ledgerDeviceDiscoveryService: LedgerDeviceDiscoveryService,
        assetSourceRegistry: AssetSourceRegistry,
    ): SelectAddressLedgerInteractor {
        return RealSelectAddressLedgerInteractor(
            legacyApp = ledgerApplication,
            migrationApp = migrationApplication,
            ledgerDeviceDiscoveryService = ledgerDeviceDiscoveryService,
            assetSourceRegistry = assetSourceRegistry
        )
    }

    @Provides
    @FeatureScope
    fun provideMetadataShortenerService(
        chainRegistry: ChainRegistry,
        rpcCalls: RpcCalls,
    ): MetadataShortenerService {
        return RealMetadataShortenerService(chainRegistry, rpcCalls)
    }
}
