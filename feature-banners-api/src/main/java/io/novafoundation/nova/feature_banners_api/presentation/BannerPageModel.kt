package io.novafoundation.nova.feature_banners_api.presentation

import android.graphics.drawable.Drawable

class BannerPageModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val image: ClipableImage,
    val background: Drawable,
    val actionUrl: String?
)

class ClipableImage(val drawable: Drawable, val clip: Boolean)
