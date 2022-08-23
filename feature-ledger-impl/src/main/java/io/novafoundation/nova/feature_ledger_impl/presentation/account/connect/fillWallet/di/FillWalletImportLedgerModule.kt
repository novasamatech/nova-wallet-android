package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.fillWallet.di

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
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_ledger_impl.domain.account.connect.fillWallet.FillWalletImportLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.connect.fillWallet.RealFillWalletImportLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.fillWallet.FillWalletImportLedgerViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class FillWalletImportLedgerModule {

    @Provides
    @ScreenScope
    fun provideInteractor(chainRegistry: ChainRegistry): FillWalletImportLedgerInteractor {
        return RealFillWalletImportLedgerInteractor(chainRegistry)
    }

    @Provides
    @IntoMap
    @ViewModelKey(FillWalletImportLedgerViewModel::class)
    fun provideViewModel(
        router: LedgerRouter,
        interactor: FillWalletImportLedgerInteractor,
        addressIconGenerator: AddressIconGenerator,
        resourceManager: ResourceManager,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory
    ): ViewModel {
        return FillWalletImportLedgerViewModel(
            router = router,
            interactor = interactor,
            addressIconGenerator = addressIconGenerator,
            resourceManager = resourceManager,
            actionAwaitableMixin = actionAwaitableMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): FillWalletImportLedgerViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(FillWalletImportLedgerViewModel::class.java)
    }
}
