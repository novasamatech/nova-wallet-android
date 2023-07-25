package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.WalletConnectSessionInteractor
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.common.WalletConnectSessionMapper
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.WalletConnectSessionsPayload
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.WalletConnectSessionsViewModel

@Module(includes = [ViewModelModule::class])
class WalletConnectSessionsModule {

    @Provides
    @IntoMap
    @ViewModelKey(WalletConnectSessionsViewModel::class)
    fun provideViewModel(
        router: WalletConnectRouter,
        interactor: WalletConnectSessionInteractor,
        walletUiUseCase: WalletUiUseCase,
        walletConnectSessionMapper: WalletConnectSessionMapper,
        walletConnectSessionsPayload: WalletConnectSessionsPayload
    ): ViewModel {
        return WalletConnectSessionsViewModel(
            router = router,
            interactor = interactor,
            walletUiUseCase = walletUiUseCase,
            walletConnectSessionMapper = walletConnectSessionMapper,
            walletConnectSessionsPayload = walletConnectSessionsPayload
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): WalletConnectSessionsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(WalletConnectSessionsViewModel::class.java)
    }
}
