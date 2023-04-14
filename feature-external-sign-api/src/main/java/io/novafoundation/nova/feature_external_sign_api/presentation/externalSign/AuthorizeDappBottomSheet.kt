package io.novafoundation.nova.feature_external_sign_api.presentation.externalSign

import android.content.Context
import android.os.Bundle
import coil.ImageLoader
import io.novafoundation.nova.common.utils.postToSelf
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_external_sign_api.R
import io.novafoundation.nova.feature_external_sign_api.presentation.dapp.showDAppIcon
import kotlinx.android.synthetic.main.bottom_sheet_confirm_authorize.confirmAuthorizeDappDApp
import kotlinx.android.synthetic.main.bottom_sheet_confirm_authorize.confirmAuthorizeDappIcon
import kotlinx.android.synthetic.main.bottom_sheet_confirm_authorize.confirmAuthorizeDappTitle
import kotlinx.android.synthetic.main.bottom_sheet_confirm_authorize.confirmAuthorizeDappWallet

class AuthorizeDappBottomSheet(
    context: Context,
    private val payload: Payload,
    onConfirm: () -> Unit,
    onDeny: () -> Unit,
    private val imageLoader: ImageLoader
) : ConfirmDAppActionBottomSheet(
    context = context,
    onConfirm = onConfirm,
    onDeny = onDeny
) {

    class Payload(
        val dAppUrl: String,
        val title: String,
        val dAppIconUrl: String?,
        val walletModel: WalletModel,
    )

    override val contentLayoutRes: Int = R.layout.bottom_sheet_confirm_authorize

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        confirmAuthorizeDappIcon.showDAppIcon(payload.dAppIconUrl, imageLoader)
        confirmAuthorizeDappWallet.postToSelf { showWallet(payload.walletModel) }

        confirmAuthorizeDappTitle.text = payload.title
        confirmAuthorizeDappDApp.postToSelf { showValue(payload.dAppUrl) }
    }
}
