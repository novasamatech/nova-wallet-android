package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main

import io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi.AuthorizeDAppPayload

class DappPendingConfirmation<A : DappPendingConfirmation.Action>(
    val onConfirm: () -> Unit,
    val onDeny: () -> Unit,
    val onCancel: () -> Unit,
    val action: A
) {

    sealed class Action {
        class Authorize(val content: AuthorizeDAppPayload) : Action()

        object AcknowledgePhishingAlert : Action()

        object CloseScreen : Action()
    }
}
