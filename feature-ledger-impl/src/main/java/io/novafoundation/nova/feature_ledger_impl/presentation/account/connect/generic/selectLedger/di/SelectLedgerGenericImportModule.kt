package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.selectLedger.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.location.LocationManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.mappers.LedgerDeviceFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.selectLedger.SelectLedgerGenericImportViewModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.selectLedger.SelectLedgerGenericPayload

@Module(includes = [ViewModelModule::class])
class SelectLedgerGenericImportModule {

    @Provides
    fun providePermissionAsker(
        permissionsAskerFactory: PermissionsAskerFactory,
        fragment: Fragment
    ) = permissionsAskerFactory.create(fragment)

    @Provides
    @ScreenScope
    fun provideMessageFormatter(factory: LedgerMessageFormatterFactory): LedgerMessageFormatter = factory.createGeneric()

    @Provides
    @ScreenScope
    fun provideMessageCommandFormatter(
        messageFormatter: LedgerMessageFormatter,
        messageCommandFormatterFactory: MessageCommandFormatterFactory
    ): MessageCommandFormatter = messageCommandFormatterFactory.create(messageFormatter)

    @Provides
    @IntoMap
    @ViewModelKey(SelectLedgerGenericImportViewModel::class)
    fun provideViewModel(
        discoveryService: LedgerDeviceDiscoveryService,
        permissionsAsker: PermissionsAsker.Presentation,
        bluetoothManager: BluetoothManager,
        locationManager: LocationManager,
        router: LedgerRouter,
        resourceManager: ResourceManager,
        messageFormatter: LedgerMessageFormatter,
        payload: SelectLedgerGenericPayload,
        deviceMapperFactory: LedgerDeviceFormatter,
        messageCommandFormatter: MessageCommandFormatter
    ): ViewModel {
        return SelectLedgerGenericImportViewModel(
            discoveryService = discoveryService,
            permissionsAsker = permissionsAsker,
            bluetoothManager = bluetoothManager,
            locationManager = locationManager,
            router = router,
            resourceManager = resourceManager,
            messageFormatter = messageFormatter,
            deviceMapperFactory = deviceMapperFactory,
            messageCommandFormatter = messageCommandFormatter,
            payload = payload
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): SelectLedgerGenericImportViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectLedgerGenericImportViewModel::class.java)
    }
}
