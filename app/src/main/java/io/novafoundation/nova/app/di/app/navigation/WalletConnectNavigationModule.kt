package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.walletConnect.WalletConnectScanCommunicatorImpl
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_dapp_impl.walletConnect.WalletConnectScanCommunicator

@Module
class WalletConnectNavigationModule {

    @Provides
    @ApplicationScope
    fun provideScanCommunicatorCommunicator(
        navigationHolder: NavigationHolder
    ): WalletConnectScanCommunicator = WalletConnectScanCommunicatorImpl(navigationHolder)
}
