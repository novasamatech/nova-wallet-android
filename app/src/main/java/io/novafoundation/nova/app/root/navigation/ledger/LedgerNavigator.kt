package io.novafoundation.nova.app.root.navigation.ledger

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.selectAddress.SelectAddressImportLedgerFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.selectAddress.SelectLedgerAddressPayload

class LedgerNavigator(navigationHolder: NavigationHolder) : BaseNavigator(navigationHolder), LedgerRouter {

    override fun openImportFillWallet() = performNavigation(R.id.action_startImportLedgerFragment_to_fillWalletImportLedgerFragment)

    override fun openImportSelectLedger(payload: SelectLedgerPayload) = performNavigation(
        actionId = R.id.action_fillWalletImportLedgerFragment_to_selectLedgerImportFragment,
        args = SelectLedgerFragment.getBundle(payload)
    )

    override fun openSelectImportAddress(payload: SelectLedgerAddressPayload) = performNavigation(
        actionId = R.id.action_selectLedgerImportFragment_to_selectAddressImportLedgerFragment,
        args = SelectAddressImportLedgerFragment.getBundle(payload)
    )
}
