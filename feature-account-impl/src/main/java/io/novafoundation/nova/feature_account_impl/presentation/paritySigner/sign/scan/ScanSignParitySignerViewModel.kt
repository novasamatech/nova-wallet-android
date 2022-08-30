package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan

import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitAction
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.presentation.scan.ScanQrViewModel
import io.novafoundation.nova.common.utils.SharedState
import io.novafoundation.nova.common.utils.getOrThrow
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenResponder
import io.novafoundation.nova.feature_account_api.presenatation.sign.signed
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.scan.ScanSignParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.common.QrCodeExpiredPresentableFactory
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.model.ScanSignParitySignerPayload
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.model.mapValidityPeriodFromParcel
import jp.co.soramitsu.fearless_utils.encrypt.SignatureWrapper.Sr25519
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class ScanSignParitySignerViewModel(
    private val router: AccountRouter,
    permissionsAsker: PermissionsAsker.Presentation,
    private val interactor: ScanSignParitySignerInteractor,
    private val signSharedState: SharedState<SignerPayloadExtrinsic>,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val responder: SignInterScreenResponder,
    private val payload: ScanSignParitySignerPayload,
    private val qrCodeExpiredPresentableFactory: QrCodeExpiredPresentableFactory,
) : ScanQrViewModel(permissionsAsker) {

    val invalidQrConfirmation = actionAwaitableMixinFactory.confirmingAction<Unit>()

    val qrCodeExpiredPresentable = qrCodeExpiredPresentableFactory.create(payload.request)

    private val validityPeriod = mapValidityPeriodFromParcel(payload.validityPeriod)
    val validityPeriodFlow = flowOf(validityPeriod)

    fun backClicked() {
        router.back()
    }

    fun timerFinished() {
        launch {
            qrCodeExpiredPresentable.showQrCodeExpired(validityPeriod)
        }
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
        val wrapper = Sr25519(signature)
        val response = payload.request.signed(wrapper)
        responder.respond(response)

        router.finishParitySignerFlow()
    }
}
