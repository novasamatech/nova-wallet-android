package io.novafoundation.nova.app.root.navigation.navigators.account

import io.novafoundation.nova.app.root.navigation.FlowInterScreenCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectSingleWallet.SelectSingleWalletCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectSingleWallet.SelectSingleWalletRequester
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectSingleWallet.SelectSingleWalletResponder
import io.novafoundation.nova.feature_account_impl.presentation.account.list.singleSelecting.SelectSingleWalletFragment
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter

class SelectSingleWalletCommunicatorImpl(private val router: AssetsRouter) :
    FlowInterScreenCommunicator<SelectSingleWalletRequester.Request, SelectSingleWalletResponder.Response>(),
    SelectSingleWalletCommunicator {

    override fun dispatchRequest(request: SelectSingleWalletRequester.Request) {
        router.openSelectSingleWallet(SelectSingleWalletFragment.createPayload(request))
    }
}
