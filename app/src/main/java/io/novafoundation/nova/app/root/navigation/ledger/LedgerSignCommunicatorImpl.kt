package io.novafoundation.nova.app.root.navigation.ledger

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.getBackStackEntryBefore
import io.novafoundation.nova.feature_account_api.presenatation.sign.LedgerSignCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator.Request
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator.Response
import io.novafoundation.nova.feature_ledger_impl.presentation.account.sign.SignLedgerFragment

class LedgerSignCommunicatorImpl(navigationHolder: NavigationHolder) :
    NavStackInterScreenCommunicator<Request, Response>(navigationHolder), LedgerSignCommunicator {

    override fun respond(response: Response) {
        val requester = navController.getBackStackEntryBefore(R.id.signLedgerFragment)

        saveResultTo(requester, response)
    }

    override fun openRequest(request: Request) {
        super.openRequest(request)

        val bundle = SignLedgerFragment.getBundle(request)
        navController.navigate(R.id.action_open_sign_ledger, bundle)
    }
}
