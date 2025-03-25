package io.novafoundation.nova.common.utils

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Rect
import android.graphics.drawable.Drawable

class DrawableExtension(
    private val contentDrawable: Drawable,
    private val extensionOffset: Rect
) : Drawable() {

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(
            left - extensionOffset.left,
            top - extensionOffset.top,
            right + extensionOffset.right,
            bottom + extensionOffset.bottom
        )

        contentDrawable.bounds = bounds
    }

    override fun draw(canvas: Canvas) {
        contentDrawable.draw(canvas)
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) {
        contentDrawable.colorFilter = colorFilter
    }

    override fun setTint(tintColor: Int) {
        contentDrawable.setTint(tintColor)
    }

    override fun setTintList(tint: ColorStateList?) {
        contentDrawable.setTintList(tint)
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return contentDrawable.opacity
    }
}
