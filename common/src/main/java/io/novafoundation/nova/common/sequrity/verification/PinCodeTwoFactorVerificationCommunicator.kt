package io.novafoundation.nova.common.sequrity.verification

import io.novafoundation.nova.common.navigation.InterScreenRequester
import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationResult
import io.novafoundation.nova.common.sequrity.verification.PinCodeTwoFactorVerificationRequester.Request
import io.novafoundation.nova.common.sequrity.verification.PinCodeTwoFactorVerificationResponder.Response
import kotlinx.parcelize.Parcelize

interface PinCodeTwoFactorVerificationRequester : InterScreenRequester<Request, Response> {

    @Parcelize
    class Request(val useBiometryIfEnabled: Boolean) : Parcelable
}

interface PinCodeTwoFactorVerificationResponder : InterScreenResponder<Request, Response> {

    @Parcelize
    class Response(val result: TwoFactorVerificationResult) : Parcelable
}

interface PinCodeTwoFactorVerificationCommunicator : PinCodeTwoFactorVerificationRequester, PinCodeTwoFactorVerificationResponder
