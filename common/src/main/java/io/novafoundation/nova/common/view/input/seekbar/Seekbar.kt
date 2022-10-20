package io.novafoundation.nova.common.view.input.seekbar

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import com.google.android.flexbox.FlexboxLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.setTextColorRes
import kotlinx.android.synthetic.main.view_seekbar.view.seekbarInner
import kotlinx.android.synthetic.main.view_seekbar.view.seekbarTickLabelsContainer

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

    fun setValues(values: SeekbarValues<*>) {
        seekbarTickLabelsContainer.removeAllViews()

        values.values.forEach { seekbarValue ->
            val tickLabel = tickLabelView(seekbarValue)
            seekbarTickLabelsContainer.addView(tickLabel)
        }

        seekbarInner.max = values.max
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
            setTextAppearance(R.style.TextAppearance_NovaFoundation_Regular_Caption1)

            layoutParams = FlexboxLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

            text = value.label
            setTextColorRes(value.labelColorRes)
        }
    }
}
