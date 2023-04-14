package io.novafoundation.nova.app.root.navigation.walletConnect

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectScanCommunicator

class WalletConnectScanCommunicatorImpl(
    navigationHolder: NavigationHolder
) : BaseInterScreenCommunicator<WalletConnectScanCommunicator.Request, WalletConnectScanCommunicator.Response>(navigationHolder),
    WalletConnectScanCommunicator {

    override fun openRequest(request: WalletConnectScanCommunicator.Request) {
        navController.navigate(R.id.action_walletConnectSessionsFragment_to_walletConnectScanFragment)
    }
}
