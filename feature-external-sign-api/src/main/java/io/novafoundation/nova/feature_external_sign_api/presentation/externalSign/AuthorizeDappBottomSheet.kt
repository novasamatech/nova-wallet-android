package io.novafoundation.nova.feature_external_sign_api.presentation.externalSign

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.postToSelf
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.view.showWallet
import io.novafoundation.nova.feature_external_sign_api.databinding.BottomSheetConfirmAuthorizeBinding
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

    override val contentBinder = BottomSheetConfirmAuthorizeBinding.inflate(LayoutInflater.from(context))

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

        contentBinder.confirmAuthorizeDappIcon.showDAppIcon(payload.dAppIconUrl, imageLoader)
        contentBinder.confirmAuthorizeDappWallet.postToSelf { showWallet(payload.walletModel) }

        contentBinder.confirmAuthorizeDappTitle.text = payload.title
        contentBinder.confirmAuthorizeDappDApp.postToSelf { showValue(payload.dAppUrl) }
    }
}
