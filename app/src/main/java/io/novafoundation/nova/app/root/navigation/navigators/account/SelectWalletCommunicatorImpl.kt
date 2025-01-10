package io.novafoundation.nova.app.root.navigation.navigators.account

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator.Payload
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator.Response

class SelectWalletCommunicatorImpl(
    private val navigationHoldersRegistry: NavigationHoldersRegistry
) : NavStackInterScreenCommunicator<Payload, Response>(navigationHoldersRegistry), SelectWalletCommunicator {

    override fun openRequest(request: Payload) {
        super.openRequest(request)

        navController.navigate(R.id.action_open_select_wallet)
    }
}
