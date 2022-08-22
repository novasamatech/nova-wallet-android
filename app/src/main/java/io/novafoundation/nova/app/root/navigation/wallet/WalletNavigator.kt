package io.novafoundation.nova.app.root.navigation.wallet

import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_wallet_api.presentation.WalletRouter

class WalletNavigator(navigationHolder: NavigationHolder) : BaseNavigator(navigationHolder), WalletRouter
