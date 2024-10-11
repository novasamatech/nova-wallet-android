package io.novafoundation.nova.common.view.input.seekbar

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.core.view.children
import com.google.android.flexbox.FlexboxLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.setTextColorRes

class Seekbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = VERTICAL

        View.inflate(context, R.layout.view_seekbar, this)
    }

    var progress: Int
        get() = seekbarInner.progress
        set(value) {
            seekbarInner.progress = value
        }

    init {
        addTickLabelOnLayoutListener()
    }

    fun setValues(values: SeekbarValues<*>) {
        seekbarInner.max = values.max

        seekbarTickLabelsContainer.removeAllViews()

        values.values.forEach { seekbarValue ->
            val tickLabel = tickLabelView(seekbarValue)
            seekbarTickLabelsContainer.addView(tickLabel)
        }
    }

    fun setOnProgressChangedListener(listener: (Int) -> Unit) {
        seekbarInner.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                listener(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun tickLabelView(value: SeekbarValue<*>): View {
        return TextView(context).apply {
            id = View.generateViewId()
            setTextAppearance(R.style.TextAppearance_NovaFoundation_Regular_Caption1)
            gravity = Gravity.CENTER_HORIZONTAL

            layoutParams = FlexboxLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

            text = value.label
            setTextColorRes(value.labelColorRes)
        }
    }

    private fun addTickLabelOnLayoutListener() {
        seekbarTickLabelsContainer.addOnLayoutChangeListener { viewGroup, _, _, _, _, _, _, _, _ ->
            require(viewGroup is ViewGroup)

            val seekbarSteps = seekbarInner.max
            val seekbarStartPadding = seekbarInner.paddingStart
            val seekbarEndPadding = seekbarInner.paddingEnd
            val slideZoneWidth = getParentMeasuredWidth() - (seekbarStartPadding + seekbarEndPadding)
            val seekbarStepWidth = slideZoneWidth / seekbarSteps

            viewGroup.children.toList()
                .forEachIndexed { index, view ->
                    val halfViewWidth = view.measuredWidth / 2
                    val stepOffset = seekbarStepWidth * index
                    val left = seekbarStartPadding - halfViewWidth + stepOffset
                    val top = 0
                    val right = seekbarStartPadding + halfViewWidth + stepOffset
                    val bottom = viewGroup.measuredHeight

                    view.layout(left, top, right, bottom)
                }
        }
    }

    private fun getParentMeasuredWidth(): Int {
        return measuredWidth
    }
}
