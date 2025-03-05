package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main

import io.novafoundation.nova.feature_external_sign_api.presentation.externalSign.AuthorizeDappBottomSheet

class DappPendingConfirmation<A : DappPendingConfirmation.Action>(
    val onConfirm: () -> Unit,
    val onDeny: () -> Unit,
    val onCancel: () -> Unit,
    val action: A
) {

    sealed class Action {
        class Authorize(val content: AuthorizeDappBottomSheet.Payload) : Action()

        object AcknowledgePhishingAlert : Action()
    }
}
