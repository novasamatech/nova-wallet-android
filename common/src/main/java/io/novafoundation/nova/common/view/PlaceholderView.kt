package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.getEnum
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import kotlinx.android.synthetic.main.view_placeholder.view.viewPlaceholderImage
import kotlinx.android.synthetic.main.view_placeholder.view.viewPlaceholderText

class PlaceholderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    enum class Style(val showBackground: Boolean, val backgroundColorRes: Int, val textColorRes: Int) {
        BACKGROUND_DARK(true, R.color.black_48, R.color.white_48),
        BACKGROUND_LIGHT(true, R.color.white_8, R.color.white_64),
        NO_BACKGROUND(false, R.color.black_48, R.color.white_64)
    }

    init {
        View.inflate(context, R.layout.view_placeholder, this)

        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL

        attrs?.let(::applyAttributes)
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.PlaceholderView) { typedArray ->
        val text = typedArray.getString(R.styleable.PlaceholderView_android_text)
        text?.let(::setText)

        val backgroundStyle = typedArray.getEnum(R.styleable.PlaceholderView_placeholderBackgroundStyle, Style.BACKGROUND_DARK)
        setStyle(backgroundStyle)

        val image = typedArray.getResourceIdOrNull(R.styleable.PlaceholderView_image)
        image?.let(::setImage)
    }

    fun setStyle(style: Style) {
        background = if (style.showBackground) {
            context.getRoundedCornerDrawable(style.backgroundColorRes, cornerSizeInDp = 12)
        } else {
            null
        }
        viewPlaceholderText.setTextColorRes(style.textColorRes)
    }

    fun setImage(@DrawableRes image: Int) {
        viewPlaceholderImage.setImageResource(image)
    }

    fun setText(text: String) {
        viewPlaceholderText.text = text
    }
}
