package io.novafoundation.nova.app.root.navigation.navigators.walletConnect

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.holders.MainNavigationHolder
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details.WalletConnectSessionDetailsFragment
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details.WalletConnectSessionDetailsPayload
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.WalletConnectSessionsFragment
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.WalletConnectSessionsPayload

class WalletConnectNavigator(navigationHolder: MainNavigationHolder) : BaseNavigator(navigationHolder), WalletConnectRouter {
    override fun openSessionDetails(payload: WalletConnectSessionDetailsPayload) = performNavigation(
        actionId = R.id.action_walletConnectSessionsFragment_to_walletConnectSessionDetailsFragment,
        args = WalletConnectSessionDetailsFragment.getBundle(payload)
    )

    override fun openScanPairingQrCode() = performNavigation(R.id.action_open_scanWalletConnect)

    override fun backToSettings() = performNavigation(R.id.walletConnectSessionDetailsFragment_to_settings)

    override fun openWalletConnectSessions(payload: WalletConnectSessionsPayload) = performNavigation(
        actionId = R.id.action_mainFragment_to_walletConnectGraph,
        args = WalletConnectSessionsFragment.getBundle(payload)
    )
}
