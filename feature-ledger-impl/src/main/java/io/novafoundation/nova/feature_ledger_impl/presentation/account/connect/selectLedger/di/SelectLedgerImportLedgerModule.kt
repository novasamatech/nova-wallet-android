package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.selectLedger.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.selectLedger.SelectLedgerImportViewModel

@Module(includes = [ViewModelModule::class])
class SelectLedgerImportLedgerModule {

    @Provides
    fun providePermissionAsker(
        permissionsAskerFactory: PermissionsAskerFactory,
        fragment: Fragment,
        router: LedgerRouter
    ) = permissionsAskerFactory.create(fragment, router)

    @Provides
    @IntoMap
    @ViewModelKey(SelectLedgerImportViewModel::class)
    fun provideViewModel(
        substrateApplication: SubstrateLedgerApplication,
        discoveryService: LedgerDeviceDiscoveryService,
        permissionsAsker: PermissionsAsker.Presentation,
        bluetoothManager: BluetoothManager,
        router: LedgerRouter,
    ): ViewModel {
        return SelectLedgerImportViewModel(
            substrateApplication = substrateApplication,
            discoveryService = discoveryService,
            permissionsAsker = permissionsAsker,
            bluetoothManager = bluetoothManager,
            router = router
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): SelectLedgerImportViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectLedgerImportViewModel::class.java)
    }
}
