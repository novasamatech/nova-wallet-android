package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectAddress.di

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
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.SelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.SelectLedgerAddressInterScreenCommunicator
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectAddress.SelectAddressImportLedgerViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SelectAddressImportLedgerModule {

    @Provides
    @ScreenScope
    fun provideMessageFormatter(
        screenPayload: SelectLedgerAddressPayload,
        factory: LedgerMessageFormatterFactory,
    ): LedgerMessageFormatter = factory.createLegacy(screenPayload.chainId, showAlerts = false)

    @Provides
    @IntoMap
    @ViewModelKey(SelectAddressImportLedgerViewModel::class)
    fun provideViewModel(
        router: LedgerRouter,
        interactor: SelectAddressLedgerInteractor,
        addressIconGenerator: AddressIconGenerator,
        resourceManager: ResourceManager,
        payload: SelectLedgerAddressPayload,
        chainRegistry: ChainRegistry,
        selectLedgerAddressInterScreenCommunicator: SelectLedgerAddressInterScreenCommunicator,
        messageFormatter: LedgerMessageFormatter
    ): ViewModel {
        return SelectAddressImportLedgerViewModel(
            router = router,
            interactor = interactor,
            addressIconGenerator = addressIconGenerator,
            resourceManager = resourceManager,
            payload = payload,
            chainRegistry = chainRegistry,
            responder = selectLedgerAddressInterScreenCommunicator,
            messageFormatter = messageFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): SelectAddressImportLedgerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectAddressImportLedgerViewModel::class.java)
    }
}
