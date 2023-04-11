package io.novafoundation.nova.feature_dapp_impl.walletConnect.presentation.sessions.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.walletConnect.WalletConnectScanCommunicator
import io.novafoundation.nova.feature_dapp_impl.walletConnect.domain.session.RealWalletConnectSessionInteractor
import io.novafoundation.nova.feature_dapp_impl.walletConnect.domain.session.WalletConnectSessionInteractor
import io.novafoundation.nova.feature_dapp_impl.walletConnect.presentation.sessions.WalletConnectSessionsViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.web3names.data.caip19.Caip19MatcherFactory

@Module(includes = [ViewModelModule::class])
class WalletConnectSessionsModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        caip19MatcherFactory: Caip19MatcherFactory
    ): WalletConnectSessionInteractor = RealWalletConnectSessionInteractor(
        accountRepository = accountRepository,
        chainRegistry = chainRegistry,
        caip19MatcherFactory = caip19MatcherFactory
    )

    @Provides
    @IntoMap
    @ViewModelKey(WalletConnectSessionsViewModel::class)
    fun provideViewModel(
        router: DAppRouter,
        communicator: WalletConnectScanCommunicator,
        awaitableMixinFactory: ActionAwaitableMixin.Factory,
        walletUiUseCase: WalletUiUseCase,
        interactor: WalletConnectSessionInteractor,
    ): ViewModel {
        return WalletConnectSessionsViewModel(router, communicator, awaitableMixinFactory, walletUiUseCase, interactor)
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): WalletConnectSessionsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(WalletConnectSessionsViewModel::class.java)
    }
}
