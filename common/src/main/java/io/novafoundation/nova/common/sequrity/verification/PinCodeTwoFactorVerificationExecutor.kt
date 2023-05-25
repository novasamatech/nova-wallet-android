package io.novafoundation.nova.common.sequrity.verification

import io.novafoundation.nova.common.sequrity.TwoFactorVerificationResult
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationExecutor
import io.novafoundation.nova.common.sequrity.verification.PinCodeTwoFactorVerificationRequester.Request
import io.novafoundation.nova.common.sequrity.verification.PinCodeTwoFactorVerificationResponder.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class PinCodeTwoFactorVerificationExecutor(
    private val interScreenCommunicator: PinCodeTwoFactorVerificationCommunicator
) : TwoFactorVerificationExecutor {

    override fun cancel() {
        interScreenCommunicator.respond(Response(TwoFactorVerificationResult.CANCELED))
    }

    override fun confirm() {
        interScreenCommunicator.respond(Response(TwoFactorVerificationResult.CONFIRMED))
    }

    override suspend fun runConfirmation(useBiometry: Boolean): TwoFactorVerificationResult = withContext(Dispatchers.Main) {
        val responseFlow = interScreenCommunicator.responseFlow
        interScreenCommunicator.openRequest(Request(useBiometry))
        return@withContext responseFlow.first().result
    }
}
