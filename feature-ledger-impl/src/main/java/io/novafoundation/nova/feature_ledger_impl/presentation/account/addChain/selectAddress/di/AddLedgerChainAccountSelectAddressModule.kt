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
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.LedgerAddAccountRepository
import io.novafoundation.nova.feature_ledger_impl.domain.account.addChain.AddLedgerChainAccountInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.addChain.RealAddLedgerChainAccountInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.SelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectAddress.AddLedgerChainAccountSelectAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectAddress.AddLedgerChainAccountSelectAddressViewModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class AddLedgerChainAccountSelectAddressModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        ledgerAddAccountRepository: LedgerAddAccountRepository
    ): AddLedgerChainAccountInteractor = RealAddLedgerChainAccountInteractor(ledgerAddAccountRepository)

    @Provides
    @ScreenScope
    fun provideSelectLedgerAddressPayload(
        screenPayload: AddLedgerChainAccountSelectAddressPayload
    ): SelectLedgerAddressPayload = SelectLedgerAddressPayload(
        deviceId = screenPayload.deviceId,
        chainId = screenPayload.chainId
    )

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
    ): ViewModel {
        return AddLedgerChainAccountSelectAddressViewModel(
            router = router,
            payload = payload,
            addChainAccountInteractor = addChainAccountInteractor,
            selectAddressLedgerInteractor = selectAddressLedgerInteractor,
            addressIconGenerator = addressIconGenerator,
            resourceManager = resourceManager,
            chainRegistry = chainRegistry,
            selectLedgerAddressPayload = selectLedgerAddressPayload
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): AddLedgerChainAccountSelectAddressViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(AddLedgerChainAccountSelectAddressViewModel::class.java)
    }
}
