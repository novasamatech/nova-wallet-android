package io.novafoundation.nova.feature_external_sign_api.presentation.dapp

import android.widget.ImageView
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.feature_external_sign_api.R

fun ImageView.showDAppIcon(
    url: String?,
    imageLoader: ImageLoader
) {
    load(url, imageLoader) {
        fallback(R.drawable.ic_earth)
        error(R.drawable.ic_earth)
    }
}
