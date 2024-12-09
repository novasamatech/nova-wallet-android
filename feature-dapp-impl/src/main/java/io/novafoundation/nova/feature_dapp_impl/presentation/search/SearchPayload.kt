package io.novafoundation.nova.feature_dapp_impl.presentation.search

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class SearchPayload(
    val initialUrl: String?,
    val request: Request,
    val preselectedCategoryId: String? = null
) : Parcelable {

    enum class Request {
        GO_TO_URL,
        OPEN_NEW_URL,
    }
}
