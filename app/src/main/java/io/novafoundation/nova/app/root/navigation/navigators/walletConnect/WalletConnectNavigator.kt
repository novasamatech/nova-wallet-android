package io.novafoundation.nova.app.root.navigation.navigators.walletConnect

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details.WalletConnectSessionDetailsFragment
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details.WalletConnectSessionDetailsPayload
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.WalletConnectSessionsFragment
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.WalletConnectSessionsPayload

class WalletConnectNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry
) : BaseNavigator(navigationHoldersRegistry), WalletConnectRouter {

    override fun openSessionDetails(payload: WalletConnectSessionDetailsPayload) {
        navigationBuilder(R.id.action_walletConnectSessionsFragment_to_walletConnectSessionDetailsFragment)
            .setArgs(WalletConnectSessionDetailsFragment.getBundle(payload))
            .perform()
    }

    override fun openScanPairingQrCode() {
        navigationBuilder(R.id.action_open_scanWalletConnect)
            .perform()
    }

    override fun backToSettings() {
        navigationBuilder(R.id.walletConnectSessionDetailsFragment_to_settings)
            .perform()
    }

    override fun openWalletConnectSessions(payload: WalletConnectSessionsPayload) {
        navigationBuilder(R.id.action_mainFragment_to_walletConnectGraph)
            .setArgs(WalletConnectSessionsFragment.getBundle(payload))
            .perform()
    }
}
