package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectLedger.di

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
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryServiceFactory
import io.novafoundation.nova.feature_ledger_impl.domain.migration.LedgerMigrationUseCase
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.AddChainAccountSelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectLedger.AddChainAccountSelectLedgerViewModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.mappers.LedgerDeviceMapper
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatterFactory

@Module(includes = [ViewModelModule::class])
class AddChainAccountSelectLedgerModule {

    @Provides
    fun providePermissionAsker(
        permissionsAskerFactory: PermissionsAskerFactory,
        fragment: Fragment,
        router: LedgerRouter
    ) = permissionsAskerFactory.createReturnable(fragment, router)

    @Provides
    @ScreenScope
    fun provideMessageFormatter(
        payload: AddChainAccountSelectLedgerPayload,
        factory: LedgerMessageFormatterFactory,
    ): LedgerMessageFormatter = factory.createLegacy(payload.addAccountPayload.chainId, showAlerts = false)

    @Provides
    @ScreenScope
    fun provideMessageCommandFormatter(
        messageFormatter: LedgerMessageFormatter,
        messageCommandFormatterFactory: MessageCommandFormatterFactory
    ): MessageCommandFormatter = messageCommandFormatterFactory.create(messageFormatter)

    @Provides
    @IntoMap
    @ViewModelKey(AddChainAccountSelectLedgerViewModel::class)
    fun provideViewModel(
        migrationUseCase: LedgerMigrationUseCase,
        payload: AddChainAccountSelectLedgerPayload,
        discoveryServiceFactory: LedgerDeviceDiscoveryServiceFactory,
        permissionsAsker: PermissionsAsker.Presentation,
        bluetoothManager: BluetoothManager,
        locationManager: LocationManager,
        router: LedgerRouter,
        resourceManager: ResourceManager,
        messageFormatter: LedgerMessageFormatter,
        ledgerDeviceMapper: LedgerDeviceMapper,
        messageCommandFormatter: MessageCommandFormatter
    ): ViewModel {
        return AddChainAccountSelectLedgerViewModel(
            migrationUseCase = migrationUseCase,
            discoveryServiceFactory = discoveryServiceFactory,
            permissionsAsker = permissionsAsker,
            bluetoothManager = bluetoothManager,
            locationManager = locationManager,
            router = router,
            resourceManager = resourceManager,
            payload = payload,
            messageFormatter = messageFormatter,
            ledgerDeviceMapper = ledgerDeviceMapper,
            messageCommandFormatter = messageCommandFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): AddChainAccountSelectLedgerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(AddChainAccountSelectLedgerViewModel::class.java)
    }
}
