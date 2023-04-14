package io.novafoundation.nova.app.root.navigation.walletConnect

import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter

class WalletConnectNavigator(navigationHolder: NavigationHolder) : BaseNavigator(navigationHolder), WalletConnectRouter
