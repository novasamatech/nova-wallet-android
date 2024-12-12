package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.walletConnect.ApproveSessionCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.navigators.walletConnect.WalletConnectNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve.ApproveSessionCommunicator

@Module
class WalletConnectNavigationModule {

    @Provides
    @ApplicationScope
    fun provideApproveSessionCommunicator(
        navigationHolder: SplitScreenNavigationHolder,
        automaticInteractionGate: AutomaticInteractionGate,
    ): ApproveSessionCommunicator = ApproveSessionCommunicatorImpl(navigationHolder, automaticInteractionGate)

    @ApplicationScope
    @Provides
    fun provideRouter(navigationHolder: SplitScreenNavigationHolder): WalletConnectRouter = WalletConnectNavigator(navigationHolder)
}
