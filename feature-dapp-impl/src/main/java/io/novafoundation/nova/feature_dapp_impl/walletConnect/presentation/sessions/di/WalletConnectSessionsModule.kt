package io.novafoundation.nova.feature_dapp_impl.walletConnect.presentation.sessions.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.walletConnect.WalletConnectScanCommunicator
import io.novafoundation.nova.feature_dapp_impl.walletConnect.presentation.sessions.WalletConnectSessionsViewModel

@Module(includes = [ViewModelModule::class])
class WalletConnectSessionsModule {

    @Provides
    @IntoMap
    @ViewModelKey(WalletConnectSessionsViewModel::class)
    fun provideViewModel(
        router: DAppRouter,
        communicator: WalletConnectScanCommunicator,
    ): ViewModel {
        return WalletConnectSessionsViewModel(router, communicator)
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): WalletConnectSessionsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(WalletConnectSessionsViewModel::class.java)
    }
}
