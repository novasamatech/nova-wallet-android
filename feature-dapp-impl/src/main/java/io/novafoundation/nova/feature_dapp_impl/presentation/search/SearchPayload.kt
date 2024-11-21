package io.novafoundation.nova.feature_dapp_impl.presentation.search

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class SearchPayload(
    val initialUrl: String?,
    val request: Request
) : Parcelable {

    enum class Request {
        CREATE_NEW_TAB,
        GO_TO_URL,
        OPEN_NEW_URL,
    }
}
