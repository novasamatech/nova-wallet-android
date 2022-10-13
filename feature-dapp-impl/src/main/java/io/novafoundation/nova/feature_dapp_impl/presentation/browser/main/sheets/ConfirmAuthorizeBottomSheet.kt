package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.sheets

import android.content.Context
import android.os.Bundle
import coil.ImageLoader
import io.novafoundation.nova.common.utils.postToSelf
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DappPendingConfirmation
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DappPendingConfirmation.Action
import io.novafoundation.nova.feature_dapp_api.presentation.common.showDAppIcon
import kotlinx.android.synthetic.main.bottom_sheet_confirm_authorize.confirmAuthorizeDappDApp
import kotlinx.android.synthetic.main.bottom_sheet_confirm_authorize.confirmAuthorizeDappIcon
import kotlinx.android.synthetic.main.bottom_sheet_confirm_authorize.confirmAuthorizeDappTitle
import kotlinx.android.synthetic.main.bottom_sheet_confirm_authorize.confirmAuthorizeDappWallet

class ConfirmAuthorizeBottomSheet(
    context: Context,
    confirmation: DappPendingConfirmation<Action.Authorize>,
    private val imageLoader: ImageLoader
) : ConfirmDAppActionBottomSheet<Action.Authorize>(context, confirmation) {

    override val contentLayoutRes: Int = R.layout.bottom_sheet_confirm_authorize

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = confirmation.action

        confirmAuthorizeDappIcon.showDAppIcon(action.content.dAppIconUrl, imageLoader)
        confirmAuthorizeDappWallet.postToSelf { showWallet(action.content.walletModel) }

        confirmAuthorizeDappTitle.text = action.content.title
        confirmAuthorizeDappDApp.postToSelf { showValue(action.content.dAppUrl) }
    }
}
