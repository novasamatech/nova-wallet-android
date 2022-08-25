package io.novafoundation.nova.app.root.navigation.ledger

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.SelectLedgerAddressInterScreenCommunicator
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.SelectLedgerAddressInterScreenCommunicator.Response

class SelectLedgerAddressCommunicatorImpl(navigationHolder: NavigationHolder):
    BaseInterScreenCommunicator<SelectLedgerPayload, Response>(navigationHolder),
    SelectLedgerAddressInterScreenCommunicator {

    override fun openRequest(request: SelectLedgerPayload) {
        val args = SelectLedgerFragment.getBundle(request)

        navController.navigate(R.id.action_fillWalletImportLedgerFragment_to_selectLedgerImportFragment, args)
    }

    override fun respond(response: Response) {
        val responseEntry = navController.getBackStackEntry(R.id.fillWalletImportLedgerFragment)

        saveResultTo(responseEntry, response)
    }
}
