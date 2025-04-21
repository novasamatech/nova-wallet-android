package io.novafoundation.nova.feature_account_api.data.multisig.model

sealed class MultisigAction {

    data object Signed: MultisigAction()

    data object CanReject : MultisigAction()

    data class CanApprove(val isFinalApproval: Boolean): MultisigAction()
}
