package io.novafoundation.nova.common.utils

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.graphics.minus
import androidx.core.graphics.toRectF

/**
 * Note: this implementation is very expensive (see [Canvas.saveLayerAlpha]).
 * This is useful for drawables without implemented alpha and color filter support, such as PictureDrawable.
 * In other cases it's recommended to use [Drawable.setAlpha] or [Drawable.setColorFilter] instead of this class.
 */
class AlphaDrawable(private val nestedDrawable: Drawable, private var alpha: Float) : Drawable() {

    private val layerBounds: RectF = RectF()

    init {
        bounds = Rect(nestedDrawable.bounds)
        layerBounds.set(bounds.toRectF())
    }

    override fun draw(canvas: Canvas) {
        canvas.saveLayerAlpha(layerBounds, (alpha * 255).toInt())
        nestedDrawable.draw(canvas)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        this.alpha = alpha.toFloat() / 255f
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        nestedDrawable.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return nestedDrawable.opacity
    }

    override fun getIntrinsicWidth(): Int {
        return nestedDrawable.intrinsicWidth
    }

    override fun getIntrinsicHeight(): Int {
        return nestedDrawable.intrinsicHeight
    }
}

fun Drawable.withAlphaDrawable(alpha: Float): Drawable {
    return AlphaDrawable(this, alpha)
}
