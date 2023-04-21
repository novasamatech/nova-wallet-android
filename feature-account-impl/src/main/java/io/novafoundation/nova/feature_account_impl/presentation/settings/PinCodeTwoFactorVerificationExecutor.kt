package io.novafoundation.nova.feature_account_impl.presentation.settings

import io.novafoundation.nova.common.sequrity.TwoFactorVerificationResult
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationExecutor
import io.novafoundation.nova.feature_account_impl.presentation.settings.PinCodeTwoFactorVerificationRequester.Request
import io.novafoundation.nova.feature_account_impl.presentation.settings.PinCodeTwoFactorVerificationResponder.Response
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

    override suspend fun runConfirmation(): TwoFactorVerificationResult = withContext(Dispatchers.Main) {
        val responseFlow = interScreenCommunicator.responseFlow
        interScreenCommunicator.openRequest(Request)
        return@withContext responseFlow.first().result
    }
}
