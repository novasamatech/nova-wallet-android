package io.novafoundation.nova.feature_ledger_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_api.sdk.transport.LedgerTransport
import io.novafoundation.nova.feature_ledger_api.data.repository.LedgerRepository
import io.novafoundation.nova.feature_ledger_impl.data.repository.RealLedgerRepository
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.RealSelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.SelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessagePresentable
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.SingleSheetLedgerMessagePresentable
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.RealSubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_impl.sdk.connection.ble.LedgerBleManager
import io.novafoundation.nova.feature_ledger_impl.sdk.discovery.ble.BleLedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.sdk.transport.ChunkedLedgerTransport
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry

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
    ): SubstrateLedgerApplication = RealSubstrateLedgerApplication(transport, ledgerRepository)

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
        substrateLedgerApplication: SubstrateLedgerApplication,
        ledgerDeviceDiscoveryService: LedgerDeviceDiscoveryService,
        assetSourceRegistry: AssetSourceRegistry,
    ): SelectAddressLedgerInteractor {
        return RealSelectAddressLedgerInteractor(
            substrateLedgerApplication = substrateLedgerApplication,
            ledgerDeviceDiscoveryService = ledgerDeviceDiscoveryService,
            assetSourceRegistry = assetSourceRegistry
        )
    }
}
