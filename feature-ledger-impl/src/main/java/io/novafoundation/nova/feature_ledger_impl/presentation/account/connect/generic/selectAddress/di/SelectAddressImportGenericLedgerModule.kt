package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.selectAddress.di

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
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.selectAddress.SelectAddressImportGenericLedgerViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SelectAddressImportGenericLedgerModule {

    @Provides
    @ScreenScope
    fun provideMessageFormatter(
        factory: LedgerMessageFormatterFactory,
    ): LedgerMessageFormatter = factory.createGeneric()

    @Provides
    @ScreenScope
    fun provideMessageCommandFormatter(
        messageFormatter: LedgerMessageFormatter,
        messageCommandFormatterFactory: MessageCommandFormatterFactory
    ): MessageCommandFormatter = messageCommandFormatterFactory.create(messageFormatter)

    @Provides
    @IntoMap
    @ViewModelKey(SelectAddressImportGenericLedgerViewModel::class)
    fun provideViewModel(
        router: LedgerRouter,
        interactor: SelectAddressLedgerInteractor,
        addressIconGenerator: AddressIconGenerator,
        resourceManager: ResourceManager,
        payload: SelectLedgerAddressPayload,
        chainRegistry: ChainRegistry,
        messageCommandFormatter: MessageCommandFormatter
    ): ViewModel {
        return SelectAddressImportGenericLedgerViewModel(
            router = router,
            interactor = interactor,
            addressIconGenerator = addressIconGenerator,
            resourceManager = resourceManager,
            payload = payload,
            chainRegistry = chainRegistry,
            messageCommandFormatter = messageCommandFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): SelectAddressImportGenericLedgerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectAddressImportGenericLedgerViewModel::class.java)
    }
}
