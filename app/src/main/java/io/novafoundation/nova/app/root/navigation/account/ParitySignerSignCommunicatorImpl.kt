package io.novafoundation.nova.app.root.navigation.account

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.getBackStackEntryBefore
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator.Request
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator.Response
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.ParitySignerSignCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show.ShowSignParitySignerFragment

class ParitySignerSignCommunicatorImpl(navigationHolder: NavigationHolder) :
    NavStackInterScreenCommunicator<Request, Response>(navigationHolder), ParitySignerSignCommunicator {

    override fun respond(response: Response) {
        val requester = navController.getBackStackEntryBefore(R.id.showSignParitySignerFragment)

        saveResultTo(requester, response)
    }

    override fun openRequest(request: Request) {
        super.openRequest(request)

        val bundle = ShowSignParitySignerFragment.getBundle(request)
        navController.navigate(R.id.action_open_sign_parity_signer, bundle)
    }
}
