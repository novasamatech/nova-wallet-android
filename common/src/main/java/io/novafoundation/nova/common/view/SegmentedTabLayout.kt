package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.InsetDrawable
import android.util.AttributeSet
import com.google.android.material.tabs.TabLayout
import io.novafoundation.nova.common.R
import kotlin.math.roundToInt

class SegmentedTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TabLayout(
    context,
    attrs,
    defStyleAttr,
) {

    private var backgroundCornerRadius: Float = 0f

    val clipPath = Path()

    init {
        applyAttrs(attrs)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        clipPath.addRoundRect(
            0f,
            0f,
            measuredWidth.toFloat(),
            measuredHeight.toFloat(),
            backgroundCornerRadius,
            backgroundCornerRadius,
            Path.Direction.CW
        )

        tabSelectedIndicator?.setBounds(
            tabSelectedIndicator?.bounds?.left ?: 0,
            0,
            100,
            measuredHeight
        )
    }

    override fun draw(canvas: Canvas) {
        canvas.clipPath(clipPath)
        super.draw(canvas)
    }

    private fun applyAttrs(attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SegmentedTabLayout)
        val tabIndicatorMargin = a.getDimension(R.styleable.SegmentedTabLayout_tabIndicatorMargin, 0f).roundToInt()
        backgroundCornerRadius = a.getDimension(R.styleable.SegmentedTabLayout_backgroundCornerRadius, 0f)
        a.recycle()

        val newTabIndicator = InsetDrawable(tabSelectedIndicator, tabIndicatorMargin)
        setSelectedTabIndicator(newTabIndicator)
    }
}
