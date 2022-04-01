package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import kotlinx.android.synthetic.main.view_placeholder.view.viewPlaceholderImage
import kotlinx.android.synthetic.main.view_placeholder.view.viewPlaceholderText

private const val SHOW_BACKGROUND_DEFAULT = true

class PlaceholderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_placeholder, this)

        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL

        attrs?.let(::applyAttributes)
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.PlaceholderView) { typedArray ->
        val text = typedArray.getString(R.styleable.PlaceholderView_android_text)
        text?.let(::setText)

        val showBackground = typedArray.getBoolean(R.styleable.PlaceholderView_showBackground, SHOW_BACKGROUND_DEFAULT)
        setShowBackground(showBackground)

        val image = typedArray.getResourceIdOrNull(R.styleable.PlaceholderView_image)
        image?.let(::setImage)
    }

    fun setShowBackground(showBackground: Boolean) {
        background = if (showBackground) {
            context.getRoundedCornerDrawable(R.color.black_48, cornerSizeInDp = 12)
        } else {
            null
        }
    }

    fun setImage(@DrawableRes image: Int) {
        viewPlaceholderImage.setImageResource(image)
    }

    fun setText(text: String) {
        viewPlaceholderText.text = text
    }
}
