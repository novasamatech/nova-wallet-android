package io.novafoundation.nova.app.root.navigation.navigators.account

import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.holders.MainNavigationHolder
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressRequester
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressResponder
import io.novafoundation.nova.feature_account_impl.presentation.account.list.selectAddress.SelectAddressFragment
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter

class SelectAddressCommunicatorImpl(private val router: AssetsRouter, navigationHolder: MainNavigationHolder) :
    NavStackInterScreenCommunicator<SelectAddressRequester.Request, SelectAddressResponder.Response>(navigationHolder),
    SelectAddressCommunicator {

    override fun openRequest(request: SelectAddressRequester.Request) {
        super.openRequest(request)

        router.openSelectAddress(SelectAddressFragment.getBundle(request))
    }
}
