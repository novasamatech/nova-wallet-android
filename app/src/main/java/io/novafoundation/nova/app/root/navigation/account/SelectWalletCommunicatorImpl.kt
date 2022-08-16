package io.novafoundation.nova.app.root.navigation.account

import io.novafoundation.nova.app.root.navigation.BaseInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.SelectWalletCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.SelectWalletRequester
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.SelectWalletResponder
import io.novafoundation.nova.feature_account_impl.presentation.account.list.selecting.SelectWalletFragment
import io.novafoundation.nova.feature_assets.presentation.WalletRouter

class SelectWalletCommunicatorImpl(private val router: WalletRouter, navigationHolder: NavigationHolder)
    : BaseInterScreenCommunicator<SelectWalletRequester.Request, SelectWalletResponder.Response>(navigationHolder),
    SelectWalletCommunicator {

    override fun openRequest(request: SelectWalletRequester.Request) {
        router.openSelectWallet(SelectWalletFragment.getBundle(request))
    }
}
