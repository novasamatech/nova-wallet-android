package io.novafoundation.nova.feature_dapp_api.presentation.addToFavorites

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class AddToFavouritesPayload(
    val url: String,
    val label: String?,
    val iconLink: String?
) : Parcelable
