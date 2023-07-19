package io.novafoundation.nova.feature_wallet_connect_impl.presentation.scan.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectService
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.scan.WalletConnectScanViewModel

@Module(includes = [ViewModelModule::class])
class WalletConnectScanModule {

    @Provides
    fun providePermissionAsker(
        permissionsAskerFactory: PermissionsAskerFactory,
        fragment: Fragment,
        router: WalletConnectRouter
    ) = permissionsAskerFactory.create(fragment, router)

    @Provides
    @IntoMap
    @ViewModelKey(WalletConnectScanViewModel::class)
    fun provideViewModel(
        router: WalletConnectRouter,
        permissionsAsker: PermissionsAsker.Presentation,
        walletConnectService: WalletConnectService
    ): ViewModel {
        return WalletConnectScanViewModel(router, permissionsAsker, walletConnectService)
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): WalletConnectScanViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(WalletConnectScanViewModel::class.java)
    }
}
