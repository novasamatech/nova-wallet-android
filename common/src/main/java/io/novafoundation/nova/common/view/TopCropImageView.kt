package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class TopCropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AppCompatImageView(context, attrs, defStyle) {

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        updateMatrix(drawable)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        drawable?.let { updateMatrix(it) }
    }

    private fun updateMatrix(drawable: Drawable?) {
        if (drawable == null) return

        val dwidth = drawable.intrinsicWidth
        val dheight = drawable.intrinsicHeight
        val vwidth = width
        val vheight = height

        if (vwidth == 0 || vheight == 0) return // Skip if size is not ready

        val matrix = imageMatrix
        val scale: Float
        var dx = 0f
        val dy = 0f

        if (dwidth * vheight > vwidth * dheight) {
            scale = vheight.toFloat() / dheight.toFloat()
            dx = (vwidth - dwidth * scale) * 0.5f
        } else {
            scale = vwidth.toFloat() / dwidth.toFloat()
        }

        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)

        imageMatrix = matrix
        invalidate()
    }
}
