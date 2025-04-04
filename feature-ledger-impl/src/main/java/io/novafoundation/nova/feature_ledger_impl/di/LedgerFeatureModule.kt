package io.novafoundation.nova.feature_ledger_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.feature_ledger_api.data.repository.LedgerRepository
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryMethods
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryServiceFactory
import io.novafoundation.nova.feature_ledger_api.sdk.transport.LedgerTransport
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.feature_ledger_impl.data.repository.RealLedgerRepository
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.RealSelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.SelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.migration.LedgerMigrationUseCase
import io.novafoundation.nova.feature_ledger_impl.domain.migration.RealLedgerMigrationUseCase
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessagePresentable
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.SingleSheetLedgerMessagePresentable
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.mappers.LedgerDeviceMapper
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.legacyApp.LegacySubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp.GenericSubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp.MigrationSubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_impl.sdk.connection.ble.LedgerBleManager
import io.novafoundation.nova.feature_ledger_impl.sdk.discovery.RealLedgerDiscoveryServiceFactory
import io.novafoundation.nova.feature_ledger_impl.sdk.discovery.ble.BleLedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.sdk.discovery.usb.UsbLedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.sdk.transport.ChunkedLedgerTransport
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

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
    fun provideGenericLedgerApplication(
        transport: LedgerTransport,
        chainRegistry: ChainRegistry,
        ledgerRepository: LedgerRepository,
        metadataShortenerService: MetadataShortenerService
    ) = GenericSubstrateLedgerApplication(
        transport = transport,
        chainRegistry = chainRegistry,
        metadataShortenerService = metadataShortenerService,
        ledgerRepository = ledgerRepository
    )

    @Provides
    @FeatureScope
    fun provideLedgerMessageFormatterFactory(
        resourceManager: ResourceManager,
        migrationTracker: LedgerMigrationTracker,
        chainRegistry: ChainRegistry,
        appLinksProvider: AppLinksProvider,
    ): LedgerMessageFormatterFactory {
        return LedgerMessageFormatterFactory(resourceManager, migrationTracker, chainRegistry, appLinksProvider)
    }

    @Provides
    @FeatureScope
    fun provideLedgerMigrationUseCase(
        ledgerMigrationTracker: LedgerMigrationTracker,
        migrationApp: MigrationSubstrateLedgerApplication,
        legacyApp: LegacySubstrateLedgerApplication,
        genericApp: GenericSubstrateLedgerApplication,
    ): LedgerMigrationUseCase {
        return RealLedgerMigrationUseCase(ledgerMigrationTracker, migrationApp, legacyApp, genericApp)
    }

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
    fun provideDeviceDiscoveryServiceFactory(
        bleLedgerDeviceDiscoveryService: BleLedgerDeviceDiscoveryService,
        usbLedgerDeviceDiscoveryService: UsbLedgerDeviceDiscoveryService
    ): LedgerDeviceDiscoveryServiceFactory = RealLedgerDiscoveryServiceFactory(
        bleLedgerDeviceDiscoveryService,
        usbLedgerDeviceDiscoveryService
    )

    @Provides
    @FeatureScope
    fun provideDeviceDiscoveryService(
        ledgerDeviceDiscoveryServiceFactory: LedgerDeviceDiscoveryServiceFactory
    ): LedgerDeviceDiscoveryService = ledgerDeviceDiscoveryServiceFactory.create(DiscoveryMethods.all())

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
        migrationUseCase: LedgerMigrationUseCase,
        ledgerDeviceDiscoveryService: LedgerDeviceDiscoveryService,
        assetSourceRegistry: AssetSourceRegistry,
    ): SelectAddressLedgerInteractor {
        return RealSelectAddressLedgerInteractor(
            migrationUseCase = migrationUseCase,
            ledgerDeviceDiscoveryService = ledgerDeviceDiscoveryService,
            assetSourceRegistry = assetSourceRegistry
        )
    }

    @Provides
    @FeatureScope
    fun provideLedgerDeviceMapper(resourceManager: ResourceManager): LedgerDeviceMapper {
        return LedgerDeviceMapper(resourceManager)
    }

    @Provides
    @FeatureScope
    fun provideMessageCommandFormatterFactory(
        resourceManager: ResourceManager,
        deviceMapper: LedgerDeviceMapper
    ) = MessageCommandFormatterFactory(resourceManager, deviceMapper)
}
