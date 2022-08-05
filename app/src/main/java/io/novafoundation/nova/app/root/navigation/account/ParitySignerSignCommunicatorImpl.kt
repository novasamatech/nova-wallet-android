package io.novafoundation.nova.app.root.navigation.account

import android.annotation.SuppressLint
import androidx.annotation.IdRes
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenCommunicator.Request
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenCommunicator.Response
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show.ShowSignParitySignerFragment

class ParitySignerSignCommunicatorImpl(navigationHolder: NavigationHolder) :
    BaseInterScreenCommunicator<Request, Response>(navigationHolder), ParitySignerSignInterScreenCommunicator {

    override fun respond(response: Response) {
        val requester = navController.getBackStackEntryBefore(R.id.showSignParitySignerFragment)

        saveResultTo(requester, response)
    }

    override fun openRequest(request: Request) {
        val bundle = ShowSignParitySignerFragment.getBundle(request)

        navController.navigate(R.id.action_open_sign_parity_signer, bundle)
    }
}

@SuppressLint("RestrictedApi")
private fun NavController.getBackStackEntryBefore(@IdRes id: Int): NavBackStackEntry {
    val initial = getBackStackEntry(id)
    val backStack = backStack.toList()

    val initialIndex = backStack.indexOf(initial)

    var previousIndex = initialIndex - 1

    // ignore nav graphs
    while (previousIndex > 0 && backStack[previousIndex].destination is NavGraph) {
        previousIndex--
    }

    return backStack[previousIndex]
}
