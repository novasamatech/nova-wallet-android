package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.selectAddress.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.domain.account.connect.selectAddress.RealSelectAddressImportLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.connect.selectAddress.SelectAddressImportLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.SelectLedgerAddressInterScreenCommunicator
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.selectAddress.SelectAddressImportLedgerViewModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SelectAddressImportLedgerModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        substrateLedgerApplication: SubstrateLedgerApplication,
        ledgerDeviceDiscoveryService: LedgerDeviceDiscoveryService,
        assetSourceRegistry: AssetSourceRegistry,
    ): SelectAddressImportLedgerInteractor {
        return RealSelectAddressImportLedgerInteractor(
            substrateLedgerApplication = substrateLedgerApplication,
            ledgerDeviceDiscoveryService = ledgerDeviceDiscoveryService,
            assetSourceRegistry = assetSourceRegistry
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(SelectAddressImportLedgerViewModel::class)
    fun provideViewModel(
        router: LedgerRouter,
        interactor: SelectAddressImportLedgerInteractor,
        addressIconGenerator: AddressIconGenerator,
        resourceManager: ResourceManager,
        payload: SelectLedgerAddressPayload,
        chainRegistry: ChainRegistry,
        selectLedgerAddressInterScreenCommunicator: SelectLedgerAddressInterScreenCommunicator,
    ): ViewModel {
        return SelectAddressImportLedgerViewModel(
            router = router,
            interactor = interactor,
            addressIconGenerator = addressIconGenerator,
            resourceManager = resourceManager,
            payload = payload,
            chainRegistry = chainRegistry,
            responder = selectLedgerAddressInterScreenCommunicator
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): SelectAddressImportLedgerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectAddressImportLedgerViewModel::class.java)
    }
}
