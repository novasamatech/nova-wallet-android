package io.novafoundation.nova.app.root.navigation.account

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.FlowInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator.Payload
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator.Response

class SelectWalletCommunicatorImpl(
    private val navigationHolder: NavigationHolder,
): FlowInterScreenCommunicator<Payload, Response>(), SelectWalletCommunicator {

    override fun dispatchRequest(request: Payload) {
       navigationHolder.navController!!.navigate(R.id.action_open_select_wallet)
    }
}
