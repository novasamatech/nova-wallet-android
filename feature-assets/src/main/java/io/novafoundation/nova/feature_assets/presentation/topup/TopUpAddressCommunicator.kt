package io.novafoundation.nova.feature_assets.presentation.topup

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import kotlinx.android.parcel.Parcelize

interface TopUpAddressRequester : InterScreenRequester<TopUpAddressPayload, TopUpAddressResponder.Response>

interface TopUpAddressResponder : InterScreenResponder<TopUpAddressPayload, TopUpAddressResponder.Response> {
    sealed interface Response : Parcelable {

        @Parcelize
        object Success : Response

        @Parcelize
        object Cancel : Response
    }
}

interface TopUpAddressCommunicator : TopUpAddressRequester, TopUpAddressResponder
