package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.modules.shared.PermissionAskerForFragmentModule
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.location.LocationManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.domain.migration.LedgerMigrationUseCase
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.mappers.LedgerDeviceFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger.SelectLedgerLegacyPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger.SelectLedgerLegacyImportViewModel

@Module(includes = [ViewModelModule::class, PermissionAskerForFragmentModule::class])
class SelectLedgerImportLedgerModule {

    @Provides
    @ScreenScope
    fun provideMessageFormatter(
        selectLedgerPayload: SelectLedgerLegacyPayload,
        factory: LedgerMessageFormatterFactory,
    ): LedgerMessageFormatter = factory.createLegacy(selectLedgerPayload.chainId, showAlerts = false)

    @Provides
    @ScreenScope
    fun provideMessageCommandFormatter(
        messageFormatter: LedgerMessageFormatter,
        messageCommandFormatterFactory: MessageCommandFormatterFactory
    ): MessageCommandFormatter = messageCommandFormatterFactory.create(messageFormatter)

    @Provides
    @IntoMap
    @ViewModelKey(SelectLedgerLegacyImportViewModel::class)
    fun provideViewModel(
        migrationUseCase: LedgerMigrationUseCase,
        selectLedgerPayload: SelectLedgerLegacyPayload,
        discoveryService: LedgerDeviceDiscoveryService,
        permissionsAsker: PermissionsAsker.Presentation,
        bluetoothManager: BluetoothManager,
        locationManager: LocationManager,
        router: LedgerRouter,
        resourceManager: ResourceManager,
        messageFormatter: LedgerMessageFormatter,
        deviceMapperFactory: LedgerDeviceFormatter,
        messageCommandFormatter: MessageCommandFormatter
    ): ViewModel {
        return SelectLedgerLegacyImportViewModel(
            migrationUseCase = migrationUseCase,
            selectLedgerPayload = selectLedgerPayload,
            discoveryService = discoveryService,
            permissionsAsker = permissionsAsker,
            bluetoothManager = bluetoothManager,
            locationManager = locationManager,
            router = router,
            resourceManager = resourceManager,
            messageFormatter = messageFormatter,
            ledgerDeviceFormatter = deviceMapperFactory,
            messageCommandFormatter = messageCommandFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): SelectLedgerLegacyImportViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectLedgerLegacyImportViewModel::class.java)
    }
}
