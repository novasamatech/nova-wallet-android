package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.WalletConnectSessionInteractor
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.common.WalletConnectSessionMapper
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details.WalletConnectSessionDetailsPayload
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details.WalletConnectSessionDetailsViewModel

@Module(includes = [ViewModelModule::class])
class WalletConnectSessionDetailsModule {

    @Provides
    @IntoMap
    @ViewModelKey(WalletConnectSessionDetailsViewModel::class)
    fun provideViewModel(
        router: WalletConnectRouter,
        walletConnectSessionMapper: WalletConnectSessionMapper,
        interactor: WalletConnectSessionInteractor,
        resourceManager: ResourceManager,
        walletUiUseCase: WalletUiUseCase,
        payload: WalletConnectSessionDetailsPayload,
        walletConnectSessionsUseCase: WalletConnectSessionsUseCase
    ): ViewModel {
        return WalletConnectSessionDetailsViewModel(
            router = router,
            interactor = interactor,
            resourceManager = resourceManager,
            walletUiUseCase = walletUiUseCase,
            walletConnectSessionMapper = walletConnectSessionMapper,
            payload = payload,
            walletConnectSessionsUseCase = walletConnectSessionsUseCase
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): WalletConnectSessionDetailsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(WalletConnectSessionDetailsViewModel::class.java)
    }
}
