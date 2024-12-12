package io.novafoundation.nova.app.root.navigation.navigators.ledger

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.LedgerChainAccount
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.SelectLedgerAddressInterScreenCommunicator

class SelectLedgerAddressCommunicatorImpl(navigationHolder: SplitScreenNavigationHolder) :
    NavStackInterScreenCommunicator<SelectLedgerPayload, LedgerChainAccount>(navigationHolder),
    SelectLedgerAddressInterScreenCommunicator {

    override fun openRequest(request: SelectLedgerPayload) {
        super.openRequest(request)

        val args = SelectLedgerFragment.getBundle(request)
        navController.navigate(R.id.action_fillWalletImportLedgerFragment_to_selectLedgerImportFragment, args)
    }

    override fun respond(response: LedgerChainAccount) {
        val responseEntry = navController.getBackStackEntry(R.id.fillWalletImportLedgerFragment)

        saveResultTo(responseEntry, response)
    }
}
