package io.novafoundation.nova.common.utils

import android.widget.ImageView
import coil.ImageLoader
import coil.imageLoader
import coil.loadAny
import coil.request.ImageRequest

fun ImageView.loadOrHide(
    any: Any?,
    imageLoader: ImageLoader = context.imageLoader,
    builder: ImageRequest.Builder.() -> Unit = {}
) {
    loadAny(any, imageLoader) {
        listener(
            onSuccess = { _, _ -> makeVisible() },
            onError = { _, _ -> makeGone() }
        )
        builder()
    }
}
