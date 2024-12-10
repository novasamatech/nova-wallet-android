package io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class AddToFavouritesPayload(
    val url: String,
    val label: String?,
    val iconLink: String?
) : Parcelable
