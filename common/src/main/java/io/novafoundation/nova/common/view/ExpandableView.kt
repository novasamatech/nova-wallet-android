package io.novafoundation.nova.common.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isVisible
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible

enum class ExpandableViewState {
    COLLAPSED,
    EXPANDED
}

class ExpandableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    private var supportAnimation: Boolean = true
    private var collapsedByDefault: Boolean = false
    private var chevronResId: Int? = null
    private var expandablePartResId: Int? = null

    private val expandCollapseAnimator = ValueAnimator()

    private val chevron: View? by lazy { findViewByIdOrNull(chevronResId) }
    private val expandablePart: View? by lazy { findViewByIdOrNull(expandablePartResId) }

    private var isExpandable: Boolean = true

    init {
        applyAttributes(attrs)
        setOnClickListener { toggle() }

        expandCollapseAnimator.interpolator = AccelerateDecelerateInterpolator()
        expandCollapseAnimator.duration = 300L
        expandCollapseAnimator.addUpdateListener { animator ->
            val animatedValue = animator.animatedValue as Float

            expandablePart?.let {
                val offset = animatedValue * it.height
                it.translationY = offset
                it.clipBounds = Rect(0, -offset.toInt(), it.width, it.height)
            }

            chevron?.let {
                it.rotation = 180 * animatedValue
            }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (collapsedByDefault) {
            collapseImmediate()
        } else {
            expandImmediate()
        }
    }

    fun setImage(@DrawableRes imageRes: Int) {
        bannerImage.setImageResource(imageRes)
    }

    fun setState(state: ExpandableViewState) {
        when (state) {
            ExpandableViewState.COLLAPSED -> collapse()
            ExpandableViewState.EXPANDED -> expand()
        }
    }

    fun collapseImmediate() {
        expandablePart?.makeGone()
        chevron?.rotation = -180f
    }

    fun expandImmediate() {
        expandablePart?.makeVisible()
        chevron?.rotation = 0f
    }

    fun setExpandable(isExpandable: Boolean) {
        this.isExpandable = isExpandable
        collapseImmediate()
        chevron?.isVisible = isExpandable
    }

    private fun applyAttributes(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ExpandableView)

            supportAnimation = typedArray.getBoolean(R.styleable.ExpandableView_supportAnimation, true)
            collapsedByDefault = typedArray.getBoolean(R.styleable.ExpandableView_collapsedByDefault, false)
            chevronResId = typedArray.getResourceIdOrNull(R.styleable.ExpandableView_chevronId)
            expandablePartResId = typedArray.getResourceIdOrNull(R.styleable.ExpandableView_expandableId)

            typedArray.recycle()
        }
    }

    private fun toggle() {
        if (!isExpandable) return

        if (expandablePart?.isVisible == true) {
            collapse()
        } else {
            expand()
        }
    }

    private fun collapse() {
        if (supportAnimation) {
            expandCollapseAnimator.removeAllListeners()
            expandCollapseAnimator.setFloatValues(0f, -1f)
            expandCollapseAnimator.doOnEnd { expandablePart?.makeGone() }
            expandCollapseAnimator.start()
        } else {
            collapseImmediate()
        }
    }

    private fun expand() {
        if (supportAnimation) {
            expandCollapseAnimator.removeAllListeners()
            expandCollapseAnimator.setFloatValues(-1f, 0f)
            expandCollapseAnimator.doOnStart { expandablePart?.makeVisible() }
            expandCollapseAnimator.start()
        } else {
            expandImmediate()
        }
    }

    private fun findViewByIdOrNull(id: Int?): View? = id?.let { findViewById(it) }
}
