package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.walletConnect.WalletConnectNavigator
import io.novafoundation.nova.app.root.navigation.walletConnect.WalletConnectScanCommunicatorImpl
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectScanCommunicator

@Module
class WalletConnectNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(navigationHolder: NavigationHolder): WalletConnectRouter = WalletConnectNavigator(navigationHolder)

    @Provides
    @ApplicationScope
    fun provideScanCommunicatorCommunicator(
        navigationHolder: NavigationHolder
    ): WalletConnectScanCommunicator = WalletConnectScanCommunicatorImpl(navigationHolder)
}
