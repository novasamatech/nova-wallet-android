package io.novafoundation.nova.feature_wallet_connect_api.presentation

import coil.ImageLoader
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.WithLifecycleExtensions
import io.novafoundation.nova.feature_external_sign_api.presentation.externalSign.AuthorizeDappBottomSheet

fun <T> T.setupWalletConnectService(
    service: WalletConnectService,
    imageLoader: ImageLoader
) where T: WithLifecycleExtensions, T: WithContextExtensions {
    service.authorizeDapp.awaitableActionLiveData.observeEvent { action ->
        AuthorizeDappBottomSheet(
            context = providedContext,
            onConfirm = { action.onSuccess(true) },
            onDeny = { action.onSuccess(false) },
            payload = action.payload,
            imageLoader = imageLoader
        ).show()
    }
}
