package io.novafoundation.nova.app.root.navigation.navigators.walletConnect

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
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
        navigationBuilder().action(R.id.action_walletConnectSessionsFragment_to_walletConnectSessionDetailsFragment)
            .setArgs(WalletConnectSessionDetailsFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openScanPairingQrCode() {
        navigationBuilder().action(R.id.action_open_scanWalletConnect)
            .navigateInFirstAttachedContext()
    }

    override fun backToSettings() {
        navigationBuilder().action(R.id.walletConnectSessionDetailsFragment_to_settings)
            .navigateInFirstAttachedContext()
    }

    override fun openWalletConnectSessions(payload: WalletConnectSessionsPayload) {
        navigationBuilder().action(R.id.action_mainFragment_to_walletConnectGraph)
            .setArgs(WalletConnectSessionsFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }
}
