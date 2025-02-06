package io.novafoundation.nova.common.view.banner

import android.graphics.Rect
import android.graphics.drawable.Drawable

class BannerPageModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val image: ClipableImage,
    val background: ClipableImage,
)

class ClipableImage(val drawable: Drawable, val clipPadding: Rect)
