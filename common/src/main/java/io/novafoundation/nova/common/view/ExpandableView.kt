package io.novafoundation.nova.common.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isVisible
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import kotlinx.android.synthetic.main.view_banner.view.bannerImage

enum class ExpandableViewState {
    COLLAPSE,
    EXPAND
}

class ExpandableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    private var chevronResId: Int? = null
    private var expandablePartResId: Int? = null

    private var chevron: View? = null
    private var expandablePart: View? = null

    private val expandCollapseAnimator = ValueAnimator()

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

    fun setImage(@DrawableRes imageRes: Int) {
        bannerImage.setImageResource(imageRes)
    }

    fun setState(state: ExpandableViewState) {
        when (state) {
            ExpandableViewState.COLLAPSE -> collapse()
            ExpandableViewState.EXPAND -> expand()
        }
    }

    private fun applyAttributes(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ExpandableView)

            chevronResId = typedArray.getResourceIdOrNull(R.styleable.ExpandableView_chevronId)
            expandablePartResId = typedArray.getResourceIdOrNull(R.styleable.ExpandableView_expandableId)

            typedArray.recycle()
        }
    }

    private fun toggle() {
        if (expandablePart?.isVisible == true) {
            collapse()
        } else {
            expand()
        }
    }

    private fun collapse() {
        expandCollapseAnimator.removeAllListeners()
        expandCollapseAnimator.setFloatValues(0f, -1f)
        expandCollapseAnimator.doOnEnd { expandablePart?.makeGone() }
        expandCollapseAnimator.start()
    }

    private fun expand() {
        expandCollapseAnimator.removeAllListeners()
        expandCollapseAnimator.setFloatValues(-1f, 0f)
        expandCollapseAnimator.doOnStart { expandablePart?.makeVisible() }
        expandCollapseAnimator.start()
    }

    override fun addView(child: View, params: ViewGroup.LayoutParams?) {
        if (child.id == expandablePartResId) {
            expandablePart = child
        } else if (child.id == chevronResId) {
            chevron = child
        }

        super.addView(child, params)
    }
}
