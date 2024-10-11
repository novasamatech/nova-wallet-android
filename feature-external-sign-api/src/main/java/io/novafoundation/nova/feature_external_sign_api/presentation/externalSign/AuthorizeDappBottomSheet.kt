package io.novafoundation.nova.feature_external_sign_api.presentation.externalSign

import android.content.Context
import android.os.Bundle
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.postToSelf
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.showWallet
import io.novafoundation.nova.feature_external_sign_api.R
import io.novafoundation.nova.feature_external_sign_api.presentation.dapp.showDAppIcon

import kotlinx.coroutines.launch

class AuthorizeDappBottomSheet(
    context: Context,
    private val payload: Payload,
    onConfirm: () -> Unit,
    onDeny: () -> Unit,
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

    private val interactionGate: AutomaticInteractionGate

    private val imageLoader: ImageLoader

    override val contentLayoutRes: Int = R.layout.bottom_sheet_confirm_authorize

    init {
        FeatureUtils.getCommonApi(context).let { api ->
            interactionGate = api.automaticInteractionGate
            imageLoader = api.imageLoader()
        }
    }

    override fun show() {
        launch {
            interactionGate.awaitInteractionAllowed()

            super.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        confirmAuthorizeDappIcon.showDAppIcon(payload.dAppIconUrl, imageLoader)
        confirmAuthorizeDappWallet.postToSelf { showWallet(payload.walletModel) }

        confirmAuthorizeDappTitle.text = payload.title
        confirmAuthorizeDappDApp.postToSelf { showValue(payload.dAppUrl) }
    }
}
