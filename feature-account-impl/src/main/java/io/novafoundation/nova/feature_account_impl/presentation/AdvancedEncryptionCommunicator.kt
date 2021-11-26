package io.novafoundation.nova.feature_account_impl.presentation

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionCommunicator.Request
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionCommunicator.Response
import kotlinx.android.parcel.Parcelize

interface AdvancedEncryptionCommunicator : InterScreenCommunicator<Request, Response> {

    @Parcelize
    class Response : Parcelable

    @Parcelize
    class Request : Parcelable
}
