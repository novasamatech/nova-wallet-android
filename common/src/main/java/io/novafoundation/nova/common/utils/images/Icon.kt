package io.novafoundation.nova.common.utils.images

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import coil.ImageLoader
import coil.load
import coil.request.ImageRequest

sealed class Icon {

    class FromLink(val data: String) : Icon()

    class FromDrawable(val data: Drawable) : Icon()

    class FromDrawableRes(@DrawableRes val res: Int) : Icon()
}

typealias ExtraImageRequestBuilding = ImageRequest.Builder.() -> Unit

fun ImageView.setIcon(icon: Icon, imageLoader: ImageLoader, builder: ExtraImageRequestBuilding = {}) {
    when (icon) {
        is Icon.FromDrawable -> load(icon.data, imageLoader, builder)
        is Icon.FromLink -> load(icon.data, imageLoader, builder)
        is Icon.FromDrawableRes -> load(icon.res, imageLoader, builder)
    }
}

fun Drawable.asIcon() = Icon.FromDrawable(this)
fun @receiver:DrawableRes Int.asIcon() = Icon.FromDrawableRes(this)
fun String.asIcon() = Icon.FromLink(this)
