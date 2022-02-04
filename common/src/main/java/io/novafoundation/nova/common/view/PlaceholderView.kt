package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import kotlinx.android.synthetic.main.view_placeholder.view.viewPlaceholderText

class PlaceholderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_placeholder, this)

        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL

        background = context.getRoundedCornerDrawable(R.color.black_48, cornerSizeInDp = 12)

        attrs?.let(::applyAttributes)
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.PlaceholderView) { typedArray ->
        val text = typedArray.getString(R.styleable.PlaceholderView_android_text)
        text?.let(::setText)
    }

    fun setText(text: String) {
        viewPlaceholderText.text = text
    }
}
