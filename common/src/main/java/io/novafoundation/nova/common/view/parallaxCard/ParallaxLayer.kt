package io.novafoundation.nova.common.view.parallaxCard

import android.graphics.Bitmap
import androidx.annotation.DrawableRes

class ParallaxLayer(
    val id: String,
    @DrawableRes val bitmapId: Int,
    val alpha: Float,
    val withHighlighting: Boolean,
    val blurRadius: Int,
    val travelVector: TravelVector
) {
    var layerBitmap: BitmapWithRect? = null
    var blurredLayerBitmap: BitmapWithRect? = null

    fun onReady(
        bitmapWithRect: Bitmap,
        blurredBitmapWithRect: Bitmap
    ) {
        this.layerBitmap = bitmapWithRect.toBitmapWithRect()
        this.blurredLayerBitmap = blurredBitmapWithRect.toBitmapWithRect()
    }

    fun isNotReady(): Boolean {
        return layerBitmap == null || blurredLayerBitmap == null
    }
}
