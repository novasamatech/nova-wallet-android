package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan

import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitAction
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.presentation.scan.ScanQrViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.SharedState
import io.novafoundation.nova.common.utils.getOrThrow
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.scan.ScanSignParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenResponder
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.model.ScanSignParitySignerPayload
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.signed
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic

class ScanSignParitySignerViewModel(
    private val router: AccountRouter,
    permissionsAsker: PermissionsAsker.Presentation,
    private val resourceManager: ResourceManager,
    private val interactor: ScanSignParitySignerInteractor,
    private val signSharedState: SharedState<SignerPayloadExtrinsic>,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val responder: ParitySignerSignInterScreenResponder,
    private val payload: ScanSignParitySignerPayload
) : ScanQrViewModel(permissionsAsker) {

    val invalidQrConfirmation = actionAwaitableMixinFactory.confirmingAction<Unit>()

    fun backClicked() {
        router.back()
    }

    override suspend fun scanned(result: String) {
        interactor.encodeAndVerifySignature(signSharedState.getOrThrow(), result)
            .onSuccess(::respondResult)
            .onFailure {
                invalidQrConfirmation.awaitAction()

                resetScanning()
            }
    }

    private fun respondResult(signature: ByteArray) {
        val response = payload.request.signed(signature)
        responder.respond(response)

        router.finishParitySignerFlow()
    }
}
