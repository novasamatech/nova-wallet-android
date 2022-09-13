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
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectLedger.AddChainAccountSelectLedgerViewModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class AddChainAccountSelectLedgerModule {

    @Provides
    @ScreenScope
    fun provideSelectLedgerPayload(
        screenPayload: AddAccountPayload.ChainAccount
    ): SelectLedgerPayload = SelectLedgerPayload(screenPayload.chainId)

    @Provides
    fun providePermissionAsker(
        permissionsAskerFactory: PermissionsAskerFactory,
        fragment: Fragment,
        router: LedgerRouter
    ) = permissionsAskerFactory.create(fragment, router)

    @Provides
    @IntoMap
    @ViewModelKey(AddChainAccountSelectLedgerViewModel::class)
    fun provideViewModel(
        substrateApplication: SubstrateLedgerApplication,
        selectLedgerPayload: SelectLedgerPayload,
        addAccountPayload: AddAccountPayload.ChainAccount,
        discoveryService: LedgerDeviceDiscoveryService,
        permissionsAsker: PermissionsAsker.Presentation,
        bluetoothManager: BluetoothManager,
        router: LedgerRouter,
        resourceManager: ResourceManager,
        chainRegistry: ChainRegistry,
    ): ViewModel {
        return AddChainAccountSelectLedgerViewModel(
            substrateApplication = substrateApplication,
            selectLedgerPayload = selectLedgerPayload,
            discoveryService = discoveryService,
            permissionsAsker = permissionsAsker,
            bluetoothManager = bluetoothManager,
            router = router,
            resourceManager = resourceManager,
            chainRegistry = chainRegistry,
            addAccountPayload = addAccountPayload
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): AddChainAccountSelectLedgerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(AddChainAccountSelectLedgerViewModel::class.java)
    }
}
