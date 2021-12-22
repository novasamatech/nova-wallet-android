package io.novafoundation.nova.feature_dapp_impl.presentation.browser.confirm

import android.content.Context
import android.os.Bundle
import coil.ImageLoader
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.DappPendingConfirmation
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.DappPendingConfirmation.Action
import io.novafoundation.nova.feature_dapp_impl.presentation.common.showDAppIcon
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

        confirmAuthorizeDappIcon.showDAppIcon(action.dAppIconUrl, imageLoader)

        with(confirmAuthorizeDappWallet) {
            valuePrimary.compoundDrawablePadding = 8.dp(context)
            valuePrimary.setCompoundDrawablesWithIntrinsicBounds(action.walletAddressModel.image, null, null, null)

            // post to prevent secondaryValue not to hide due to early show
            post { showValue(action.walletAddressModel.nameOrAddress) }
        }

        confirmAuthorizeDappTitle.text = action.title
        with(confirmAuthorizeDappDApp) {
            post { confirmAuthorizeDappDApp.showValue(action.dAppUrl) }
        }
    }
}
