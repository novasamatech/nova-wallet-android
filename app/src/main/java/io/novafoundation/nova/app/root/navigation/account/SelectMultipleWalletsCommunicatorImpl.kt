package io.novafoundation.nova.app.root.navigation.account

import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsRequester
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsResponder
import io.novafoundation.nova.feature_account_impl.presentation.account.list.multipleSelecting.SelectMultipleWalletsFragment
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter

class SelectMultipleWalletsCommunicatorImpl(private val router: AssetsRouter, navigationHolder: NavigationHolder) :
    NavStackInterScreenCommunicator<SelectMultipleWalletsRequester.Request, SelectMultipleWalletsResponder.Response>(navigationHolder),
    SelectMultipleWalletsCommunicator {

    override fun openRequest(request: SelectMultipleWalletsRequester.Request) {
        super.openRequest(request)

        router.openSelectMultipleWallets(SelectMultipleWalletsFragment.getBundle(request))
    }
}
