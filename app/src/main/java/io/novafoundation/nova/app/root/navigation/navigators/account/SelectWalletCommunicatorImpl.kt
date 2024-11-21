package io.novafoundation.nova.app.root.navigation.navigators.account

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.holders.MainNavigationHolder
import io.novafoundation.nova.app.root.navigation.holders.NavigationHolder
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator.Payload
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator.Response

class SelectWalletCommunicatorImpl(
    private val navigationHolder: NavigationHolder,
) : NavStackInterScreenCommunicator<Payload, Response>(navigationHolder), SelectWalletCommunicator {

    override fun openRequest(request: Payload) {
        super.openRequest(request)

        navigationHolder.navController!!.navigate(R.id.action_open_select_wallet)
    }
}
