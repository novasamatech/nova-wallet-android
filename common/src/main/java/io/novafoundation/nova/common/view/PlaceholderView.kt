package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.getEnum
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import kotlinx.android.synthetic.main.view_placeholder.view.viewPlaceholderButton
import kotlinx.android.synthetic.main.view_placeholder.view.viewPlaceholderImage
import kotlinx.android.synthetic.main.view_placeholder.view.viewPlaceholderText

class PlaceholderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    enum class Style(val showBackground: Boolean, val backgroundColorRes: Int?, val textColorRes: Int) {
        BACKGROUND_PRIMARY(true, R.color.block_background, R.color.text_tertiary),
        BACKGROUND_SECONDARY(true, R.color.block_background, R.color.text_tertiary),
        NO_BACKGROUND(false, null, R.color.text_tertiary)
    }

    init {
        View.inflate(context, R.layout.view_placeholder, this)

        setPadding(16.dp(context), 16.dp(context), 16.dp(context), 32.dp(context))

        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL

        attrs?.let(::applyAttributes)
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.PlaceholderView) { typedArray ->
        val text = typedArray.getString(R.styleable.PlaceholderView_android_text)
        text?.let(::setText)

        val backgroundStyle = typedArray.getEnum(R.styleable.PlaceholderView_placeholderBackgroundStyle, Style.BACKGROUND_PRIMARY)
        setStyle(backgroundStyle)

        val image = typedArray.getResourceIdOrNull(R.styleable.PlaceholderView_image)
        image?.let(::setImage)
    }

    fun setStyle(style: Style) {
        background = if (style.showBackground) {
            context.getRoundedCornerDrawable(style.backgroundColorRes!!, cornerSizeInDp = 12)
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

    fun setText(@StringRes textRes: Int) {
        viewPlaceholderText.setText(textRes)
    }

    fun setButtonText(text: String?) {
        viewPlaceholderButton.setTextOrHide(text)
    }

    fun setButtonText(@StringRes textRes: Int) {
        setButtonText(context.getString(textRes))
    }

    fun setModel(model: PlaceholderModel) {
        setText(model.text)
        setImage(model.imageRes)
        setButtonText(model.buttonText)
    }

    fun setButtonClickListener(listener: OnClickListener?) {
        viewPlaceholderButton.setOnClickListener(listener)
    }
}

class PlaceholderModel(val text: String, @DrawableRes val imageRes: Int, val buttonText: String? = null)

fun PlaceholderView.setModelOrHide(model: PlaceholderModel?) {
    model?.let { setModel(it) }
    isVisible = model != null
}
