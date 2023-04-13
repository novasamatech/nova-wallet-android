package io.novafoundation.nova.feature_dapp_impl.walletConnect.presentation.sessions.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.caip.caip2.Caip2Resolver
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectScanCommunicator
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.KnownSessionRequestProcessor
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.RealKnownSessionRequestProcessor
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.RealWalletConnectSessionInteractor
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.WalletConnectSessionInteractor
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.WalletConnectSessionsViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class WalletConnectSessionsModule {

    @Provides
    @ScreenScope
    fun provideKnownRequestParser(gson: Gson): KnownSessionRequestProcessor = RealKnownSessionRequestProcessor(gson)

    @Provides
    @ScreenScope
    fun provideInteractor(
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        caip2Resolver: Caip2Resolver,
        knownSessionRequestProcessor: KnownSessionRequestProcessor
    ): WalletConnectSessionInteractor = RealWalletConnectSessionInteractor(
        accountRepository = accountRepository,
        chainRegistry = chainRegistry,
        caip2Resolver = caip2Resolver,
        sessionRequestParser = knownSessionRequestProcessor
    )

    @Provides
    @IntoMap
    @ViewModelKey(WalletConnectSessionsViewModel::class)
    fun provideViewModel(
        router: WalletConnectRouter,
        communicator: WalletConnectScanCommunicator,
        awaitableMixinFactory: ActionAwaitableMixin.Factory,
        walletUiUseCase: WalletUiUseCase,
        interactor: WalletConnectSessionInteractor,
        dAppSignCommunicator: ExternalSignCommunicator
    ): ViewModel {
        return WalletConnectSessionsViewModel(router, communicator, awaitableMixinFactory, walletUiUseCase, interactor, dAppSignCommunicator)
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): WalletConnectSessionsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(WalletConnectSessionsViewModel::class.java)
    }
}
