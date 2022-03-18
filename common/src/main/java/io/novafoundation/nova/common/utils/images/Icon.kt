package io.novafoundation.nova.common.utils.images

import android.graphics.drawable.Drawable
import android.widget.ImageView
import coil.ImageLoader
import coil.load

sealed class Icon {

    class FromLink(val data: String) : Icon()

    class FromDrawable(val data: Drawable) : Icon()
}

fun ImageView.setIcon(icon: Icon, imageLoader: ImageLoader) {
    when (icon) {
        is Icon.FromDrawable -> setImageDrawable(icon.data)
        is Icon.FromLink -> load(icon.data, imageLoader)
    }
}

fun Drawable.asIcon() = Icon.FromDrawable(this)
fun String.asIcon() = Icon.FromLink(this)
