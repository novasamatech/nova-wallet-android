package io.novafoundation.nova.common.presentation.masking

import android.content.res.Resources
import android.content.res.Resources.Theme
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.ColorInt
import io.novafoundation.nova.common.R

class MaskableDrawable : Drawable() {

    companion object {
        private const val DEFAULT_DOT_SIZE_DP = 6f
        private const val DEFAULT_DOT_SPACING_DP = 4f

        private fun dpToPx(dp: Float, res: Resources): Float =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.displayMetrics)
    }

    enum class Gravity(val v: Int) {
        START(0), CENTER(1), END(2);

        companion object {
            fun fromInt(v: Int) = when (v) {
                0 -> START; 1 -> CENTER; else -> END
            }
        }
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private var dotSizePx: Float = dpToPx(DEFAULT_DOT_SIZE_DP, Resources.getSystem())
    private var dotSpacingPx: Float = dpToPx(DEFAULT_DOT_SPACING_DP, Resources.getSystem())
    private var dotCount: Int = 4
    private var gravity: Gravity = Gravity.CENTER

    @ColorInt
    private var dotColorParam: Int = Color.BLACK
        set(value) {
            field = value
            paint.color = value
            invalidateSelf()
        }

    fun setDotSizePx(sizePx: Float) {
        if (sizePx != dotSizePx) {
            dotSizePx = sizePx; invalidateSelf()
        }
    }

    fun setDotSpacingPx(spacingPx: Float) {
        if (spacingPx != dotSpacingPx) {
            dotSpacingPx = spacingPx; invalidateSelf()
        }
    }

    fun setDotCount(count: Int) {
        val c = count.coerceAtLeast(1); if (c != dotCount) {
            dotCount = c; invalidateSelf()
        }
    }

    fun setDotColor(@ColorInt color: Int) {
        dotColorParam = color
    }

    fun setGravity(g: Gravity) {
        if (g != gravity) {
            gravity = g; invalidateSelf()
        }
    }

    fun setDotSizeDp(dp: Float, res: Resources) = setDotSizePx(dpToPx(dp, res))
    fun setDotSpacingDp(dp: Float, res: Resources) = setDotSpacingPx(dpToPx(dp, res))

    override fun draw(canvas: Canvas) {
        if (dotCount <= 0 || dotSizePx <= 0f) return

        val b = bounds
        val radius = dotSizePx * 0.5f
        val contentWidth = dotCount * dotSizePx + (dotCount - 1) * dotSpacingPx

        val startX = when (gravity) {
            Gravity.START -> b.left.toFloat() + radius
            Gravity.CENTER -> b.left + (b.width() - contentWidth) * 0.5f + radius
            Gravity.END -> b.right - contentWidth + radius
        }

        val cy = b.exactCenterY()
        var cx = startX
        for (i in 0 until dotCount) {
            canvas.drawCircle(cx, cy, radius, paint)
            cx += dotSizePx + dotSpacingPx
        }
    }

    override fun setAlpha(alpha: Int) {
        val a = alpha.coerceIn(0, 255)
        if (paint.alpha != a) {
            paint.alpha = a
            invalidateSelf()
        }
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicHeight(): Int = dotSizePx.toInt()
    override fun getIntrinsicWidth(): Int = (dotCount * dotSizePx + (dotCount - 1) * dotSpacingPx).toInt()

    override fun inflate(r: Resources, parser: org.xmlpull.v1.XmlPullParser, attrs: AttributeSet, theme: Theme?) {
        super.inflate(r, parser, attrs, theme)
        val a = theme?.obtainStyledAttributes(attrs, R.styleable.MaskableDrawable, 0, 0)
            ?: r.obtainAttributes(attrs, R.styleable.MaskableDrawable)
        try {
            dotSizePx = a.getDimension(
                R.styleable.MaskableDrawable_md_dotSize,
                dpToPx(DEFAULT_DOT_SIZE_DP, r)
            )
            dotSpacingPx = a.getDimension(
                R.styleable.MaskableDrawable_md_dotSpacing,
                dpToPx(DEFAULT_DOT_SPACING_DP, r)
            )
            dotColorParam = a.getColor(
                R.styleable.MaskableDrawable_md_dotColor,
                Color.BLACK
            )
            dotCount = a.getInt(R.styleable.MaskableDrawable_md_dotCount, 4).coerceAtLeast(1)
            gravity = Gravity.fromInt(a.getInt(R.styleable.MaskableDrawable_md_gravity, Gravity.CENTER.v))
            setBounds(0, dotSizePx.toInt(), dotSizePx.toInt() * dotCount + (dotCount - 1) * dotSpacingPx.toInt(), dotSizePx.toInt())
        } finally {
            a.recycle()
        }
        paint.color = dotColorParam
    }

    override fun setTint(color: Int) {
        setDotColor(color)
    }
}
