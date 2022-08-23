package io.novafoundation.nova.app.root.navigation.ledger

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter

class LedgerNavigator(navigationHolder: NavigationHolder) : BaseNavigator(navigationHolder), LedgerRouter {

    override fun openImportFillWallet() = performNavigation(R.id.action_startImportLedgerFragment_to_fillWalletImportLedgerFragment)
}
