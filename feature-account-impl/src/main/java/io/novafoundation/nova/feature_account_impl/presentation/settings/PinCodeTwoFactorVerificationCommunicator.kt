package io.novafoundation.nova.feature_account_impl.presentation.settings

import io.novafoundation.nova.common.navigation.InterScreenRequester
import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationResult
import io.novafoundation.nova.feature_account_impl.presentation.settings.PinCodeTwoFactorVerificationRequester.Request
import io.novafoundation.nova.feature_account_impl.presentation.settings.PinCodeTwoFactorVerificationResponder.Response
import kotlinx.android.parcel.Parcelize

interface PinCodeTwoFactorVerificationRequester : InterScreenRequester<Request, Response> {

    @Parcelize
    object Request : Parcelable
}

interface PinCodeTwoFactorVerificationResponder : InterScreenResponder<Request, Response> {

    @Parcelize
    class Response(val result: TwoFactorVerificationResult) : Parcelable
}

interface PinCodeTwoFactorVerificationCommunicator : PinCodeTwoFactorVerificationRequester, PinCodeTwoFactorVerificationResponder
