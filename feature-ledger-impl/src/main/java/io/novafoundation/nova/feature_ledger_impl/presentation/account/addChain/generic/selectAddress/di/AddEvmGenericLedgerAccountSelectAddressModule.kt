package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.generic.selectAddress.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.LegacyLedgerAddAccountRepository
import io.novafoundation.nova.feature_ledger_impl.di.annotations.GenericLedger
import io.novafoundation.nova.feature_ledger_impl.domain.account.addChain.generic.AddEvmAccountToGenericLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.addChain.generic.RealAddEvmAccountToGenericLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.addChain.legacy.AddLedgerChainAccountInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.addChain.legacy.RealAddLedgerChainAccountInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.SelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.generic.selectAddress.AddEvmGenericLedgerAccountSelectAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.generic.selectAddress.AddEvmGenericLedgerAccountSelectAddressViewModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.generic.selectAddress.di.AddEvmGenericLedgerAccountSelectAddressModule.BindsModule
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.legacy.selectAddress.AddLedgerChainAccountSelectAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.legacy.selectAddress.AddLedgerChainAccountSelectAddressViewModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatterFactory
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.generic.GenericLedgerEvmAlertFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

@Module(includes = [ViewModelModule::class, BindsModule::class])
class AddEvmGenericLedgerAccountSelectAddressModule {

    @Module
    interface BindsModule {

        @Binds
        fun bindInteractor(real: RealAddEvmAccountToGenericLedgerInteractor): AddEvmAccountToGenericLedgerInteractor
    }

    @Provides
    @IntoMap
    @ViewModelKey(AddEvmGenericLedgerAccountSelectAddressViewModel::class)
    fun provideViewModel(
        router: LedgerRouter,
        payload: AddEvmGenericLedgerAccountSelectAddressPayload,
        addAccountInteractor: AddEvmAccountToGenericLedgerInteractor,
        selectAddressLedgerInteractor: SelectAddressLedgerInteractor,
        addressIconGenerator: AddressIconGenerator,
        resourceManager: ResourceManager,
        chainRegistry: ChainRegistry,
        @GenericLedger messageCommandFormatter: MessageCommandFormatter,
        evmAlertFormatter: GenericLedgerEvmAlertFormatter,
    ): ViewModel {
        val selectLedgerAddressPayload = SelectLedgerAddressPayload(payload.deviceId, substrateChainId = Chain.Geneses.POLKADOT)

        return AddEvmGenericLedgerAccountSelectAddressViewModel(
            router = router,
            payload = payload,
            selectAddressLedgerInteractor = selectAddressLedgerInteractor,
            addressIconGenerator = addressIconGenerator,
            resourceManager = resourceManager,
            chainRegistry = chainRegistry,
            selectLedgerAddressPayload = selectLedgerAddressPayload,
            messageCommandFormatter = messageCommandFormatter,
            addAccountInteractor = addAccountInteractor,
            evmAlertFormatter = evmAlertFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): AddEvmGenericLedgerAccountSelectAddressViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(AddEvmGenericLedgerAccountSelectAddressViewModel::class.java)
    }
}
