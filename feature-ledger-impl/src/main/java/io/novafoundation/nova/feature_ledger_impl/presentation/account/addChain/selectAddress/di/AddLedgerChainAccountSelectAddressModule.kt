package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectAddress.di

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
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.LegacyLedgerAddAccountRepository
import io.novafoundation.nova.feature_ledger_impl.domain.account.addChain.AddLedgerChainAccountInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.addChain.RealAddLedgerChainAccountInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.SelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectAddress.AddLedgerChainAccountSelectAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectAddress.AddLedgerChainAccountSelectAddressViewModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.mappers.LedgerDeviceMapper
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class AddLedgerChainAccountSelectAddressModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        legacyLedgerAddAccountRepository: LegacyLedgerAddAccountRepository
    ): AddLedgerChainAccountInteractor = RealAddLedgerChainAccountInteractor(legacyLedgerAddAccountRepository)

    @Provides
    @ScreenScope
    fun provideSelectLedgerAddressPayload(
        screenPayload: AddLedgerChainAccountSelectAddressPayload
    ): SelectLedgerAddressPayload = SelectLedgerAddressPayload(
        deviceId = screenPayload.deviceId,
        chainId = screenPayload.chainId
    )

    @Provides
    @ScreenScope
    fun provideMessageFormatter(
        screenPayload: AddLedgerChainAccountSelectAddressPayload,
        factory: LedgerMessageFormatterFactory,
    ): LedgerMessageFormatter = factory.createLegacy(screenPayload.chainId, showAlerts = false)

    @Provides
    @ScreenScope
    fun provideMessageCommandFormatter(
        messageFormatter: LedgerMessageFormatter,
        messageCommandFormatterFactory: MessageCommandFormatterFactory
    ): MessageCommandFormatter = messageCommandFormatterFactory.create(messageFormatter)

    @Provides
    @IntoMap
    @ViewModelKey(AddLedgerChainAccountSelectAddressViewModel::class)
    fun provideViewModel(
        router: LedgerRouter,
        payload: AddLedgerChainAccountSelectAddressPayload,
        addChainAccountInteractor: AddLedgerChainAccountInteractor,
        selectAddressLedgerInteractor: SelectAddressLedgerInteractor,
        addressIconGenerator: AddressIconGenerator,
        resourceManager: ResourceManager,
        chainRegistry: ChainRegistry,
        selectLedgerAddressPayload: SelectLedgerAddressPayload,
        messageCommandFormatter: MessageCommandFormatter
    ): ViewModel {
        return AddLedgerChainAccountSelectAddressViewModel(
            router = router,
            payload = payload,
            addChainAccountInteractor = addChainAccountInteractor,
            selectAddressLedgerInteractor = selectAddressLedgerInteractor,
            addressIconGenerator = addressIconGenerator,
            resourceManager = resourceManager,
            chainRegistry = chainRegistry,
            selectLedgerAddressPayload = selectLedgerAddressPayload,
            messageCommandFormatter = messageCommandFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): AddLedgerChainAccountSelectAddressViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(AddLedgerChainAccountSelectAddressViewModel::class.java)
    }
}
