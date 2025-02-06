package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import androidx.appcompat.widget.AppCompatImageView

class ClipableImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    init {
        clipToOutline = true
    }

    fun setClipPadding(clipPadding: Rect) {
        outlineProvider = ClipOutlineProvider(clipPadding)
        invalidate()
    }

    private class ClipOutlineProvider(private val clipPadding: Rect) : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRect(
                0 + clipPadding.left,
                0 + clipPadding.top,
                view.width - clipPadding.right,
                view.height - clipPadding.bottom
            )
        }
    }
}
