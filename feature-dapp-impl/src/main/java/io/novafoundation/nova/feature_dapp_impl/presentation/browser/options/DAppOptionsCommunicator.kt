package io.novafoundation.nova.feature_dapp_impl.presentation.browser.options

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import kotlinx.android.parcel.Parcelize

interface DAppOptionsRequester : InterScreenRequester<DAppOptionsPayload, DAppOptionsCommunicator.Response>

interface DAppOptionsResponder : InterScreenResponder<DAppOptionsPayload, DAppOptionsCommunicator.Response>

interface DAppOptionsCommunicator : DAppOptionsRequester, DAppOptionsResponder {

    interface Response : Parcelable {
        @Parcelize
        object FavoriteClick : Response

        @Parcelize
        object DesktopModeClick : Response
    }
}
