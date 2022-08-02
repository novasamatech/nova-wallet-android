package io.novafoundation.nova.app.root.navigation.account

import android.annotation.SuppressLint
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenCommunicator.Request
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenCommunicator.Response

class ParitySignerSignCommunicatorImpl(navigationHolder: NavigationHolder) :
    BaseInterScreenCommunicator<Request, Response>(navigationHolder), ParitySignerSignInterScreenCommunicator {

    @SuppressLint("RestrictedApi")
    override fun respond(response: Response) {
        val startSignFlowEntry = navController.getBackStackEntry(R.id.showSignParitySignerFragment)
        val backStack = navController.backStack.toList()

        val index = backStack.indexOf(startSignFlowEntry)
        val requesterIndex = index - 1

        val requester = backStack[requesterIndex]

        saveResultTo(requester, response)
    }

    override fun openRequest(request: Request) {
        navController.navigate(R.id.action_open_sign_parity_signer)
    }
}
