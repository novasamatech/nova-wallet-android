package io.novafoundation.nova.common.utils.images

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import coil.ImageLoader
import coil.load

sealed class Icon {

    class FromLink(val data: String) : Icon()

    class FromDrawable(val data: Drawable) : Icon()

    class FromDrawableRes(@DrawableRes val res: Int): Icon()
}

fun ImageView.setIcon(icon: Icon, imageLoader: ImageLoader) {
    when (icon) {
        is Icon.FromDrawable -> setImageDrawable(icon.data)
        is Icon.FromLink -> load(icon.data, imageLoader)
        is Icon.FromDrawableRes -> setImageResource(icon.res)
    }
}

fun Drawable.asIcon() = Icon.FromDrawable(this)
fun @receiver:DrawableRes Int.asIcon() = Icon.FromDrawableRes(this)
fun String.asIcon() = Icon.FromLink(this)
