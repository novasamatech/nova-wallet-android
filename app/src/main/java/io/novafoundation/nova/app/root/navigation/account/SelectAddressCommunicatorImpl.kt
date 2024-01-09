package io.novafoundation.nova.app.root.navigation.account

import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressRequester
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressResponder
import io.novafoundation.nova.feature_account_impl.presentation.account.list.selectAddress.SelectAddressFragment
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter

class SelectAddressCommunicatorImpl(private val router: AssetsRouter, navigationHolder: NavigationHolder) :
    NavStackInterScreenCommunicator<SelectAddressRequester.Request, SelectAddressResponder.Response>(navigationHolder),
    SelectAddressCommunicator {

    override fun openRequest(request: SelectAddressRequester.Request) {
        super.openRequest(request)

        router.openSelectAddress(SelectAddressFragment.getBundle(request))
    }
}
