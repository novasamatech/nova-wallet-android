package io.novafoundation.nova.common.view

import android.animation.Animator.AnimatorListener
import android.animation.LayoutTransition
import android.animation.TimeAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.animation.addListener
import androidx.core.view.isVisible
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.getParcelableCompat
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.useAttributes
import kotlinx.android.synthetic.main.view_tap_to_view_container.view.tapToViewContainer
import kotlinx.android.synthetic.main.view_tap_to_view_container.view.tapToViewHiddenContent
import kotlinx.android.synthetic.main.view_tap_to_view_container.view.tapToViewSbutitle
import kotlinx.android.synthetic.main.view_tap_to_view_container.view.tapToViewTitle
import kotlin.math.roundToInt


private const val SUPER_STATE = "super_state"
private const val REVEAL_CONTAINER_VISIBILITY = "reveal_container_visibility"

open class TapToViewContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var onShowContentClicked: OnClickListener? = null

    private var isContentVisible = false

    private var cornerRadius: Float = 0f
    private val clipPath = Path()
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = context.getColor(R.color.container_border)
        strokeWidth = 2.dpF(context) // Actually it will be drawn as 1dp, since half of the stroke will be clipped by path
    }

    init {
        inflate(context, R.layout.view_tap_to_view_container, this)
        layoutTransition = LayoutTransition() // To animate this layout size change and children visibility

        tapToViewContainer.setOnClickListener {
            isContentVisible = true
            onShowContentClicked?.onClick(this)
            tapToViewContainer.visibility = GONE
            requestLayout()
        }

        attrs?.let(::applyAttrs)

        setWillNotDraw(false)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (isContentVisible) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        } else {
            val width = MeasureSpec.getSize(widthMeasureSpec)

            // Set set the height of the view as tapToViewContainer background height considering the width of the view
            val tapToViewBackgroundHeight = tapToViewContainer.background?.intrinsicHeight ?: 0
            val tabToViewBackgroundWidth = tapToViewContainer.background?.intrinsicWidth ?: 0
            val height = tapToViewBackgroundHeight * (width.toFloat() / tabToViewBackgroundWidth.toFloat())

            val widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec(height.roundToInt(), MeasureSpec.EXACTLY)

            super.onMeasure(widthSpec, heightSpec)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        clipPath.reset()
        clipPath.addRoundRect(0f, 0f, width.toFloat(), height.toFloat(), cornerRadius, cornerRadius, Path.Direction.CW)
    }

    /**
     * To add a view to the container that we want to hide
     */
    override fun addView(child: View, params: ViewGroup.LayoutParams) {
        if (child.id == R.id.tapToViewContainer || child.id == R.id.tapToViewHiddenContent) {
            super.addView(child, params)
        } else {
            tapToViewHiddenContent.addView(child, params)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()

        return Bundle().apply {
            putParcelable(SUPER_STATE, superState)
            putBoolean(REVEAL_CONTAINER_VISIBILITY, isContentVisible)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            super.onRestoreInstanceState(state.getParcelableCompat<Parcelable>(SUPER_STATE))

            isContentVisible = state.getBoolean(REVEAL_CONTAINER_VISIBILITY)
            tapToViewContainer.isVisible = !isContentVisible
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.clipPath(clipPath)
        super.draw(canvas)
        canvas.drawPath(clipPath, strokePaint)
    }

    fun setTitleOrHide(title: String?) {
        tapToViewTitle.setTextOrHide(title)
    }

    fun setSubtitleOrHide(subtitle: String?) {
        tapToViewSbutitle.setTextOrHide(subtitle)
    }

    fun setTapToViewBackground(@DrawableRes background: Int) {
        tapToViewContainer.setBackgroundResource(background)
        requestLayout()
    }

    fun setCardCornerRadius(radius: Float) {
        cornerRadius = radius
        requestLayout()
    }

    fun showContent(show: Boolean) {
        isContentVisible = show
        tapToViewContainer.isVisible = !show
        requestLayout()
    }

    fun onContentShownListener(listener: OnClickListener) {
        onShowContentClicked = listener
    }

    private fun applyAttrs(attributeSet: AttributeSet) = context.useAttributes(
        attributeSet,
        R.styleable.TapToViewContainer
    ) {
        val title = it.getString(R.styleable.TapToViewContainer_title)
        val subtitle = it.getString(R.styleable.TapToViewContainer_subtitle)
        val tapToRevealBackground = it.getResourceIdOrNull(R.styleable.TapToViewContainer_tapToViewBackground) ?: android.R.color.black
        cornerRadius = it.getDimension(R.styleable.TapToViewContainer_cornerRadius, 0f)

        setTitleOrHide(title)
        setSubtitleOrHide(subtitle)
        setTapToViewBackground(tapToRevealBackground)
    }
}
