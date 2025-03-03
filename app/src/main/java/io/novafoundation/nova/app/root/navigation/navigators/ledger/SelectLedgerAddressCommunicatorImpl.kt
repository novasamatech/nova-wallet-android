package io.novafoundation.nova.app.root.navigation.navigators.ledger

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger.SelectLedgerLegacyPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.LedgerChainAccount
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.SelectLedgerAddressInterScreenCommunicator
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger.SelectLedgerLegacyImportFragment

class SelectLedgerAddressCommunicatorImpl(navigationHoldersRegistry: NavigationHoldersRegistry) :
    NavStackInterScreenCommunicator<SelectLedgerLegacyPayload, LedgerChainAccount>(navigationHoldersRegistry),
    SelectLedgerAddressInterScreenCommunicator {

    override fun openRequest(request: SelectLedgerLegacyPayload) {
        super.openRequest(request)

        val args = SelectLedgerLegacyImportFragment.getBundle(request)
        navController.navigate(R.id.action_fillWalletImportLedgerFragment_to_selectLedgerImportFragment, args)
    }

    override fun respond(response: LedgerChainAccount) {
        val responseEntry = navController.getBackStackEntry(R.id.fillWalletImportLedgerFragment)

        saveResultTo(responseEntry, response)
    }
}
