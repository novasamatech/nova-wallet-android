package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewPlaceholderBinding
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.getColorOrNull
import io.novafoundation.nova.common.utils.getEnum
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setImageTint
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable

class PlaceholderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    enum class Style(val showBackground: Boolean, val backgroundColorRes: Int?, val textColorRes: Int) {
        BACKGROUND_PRIMARY(true, R.color.block_background, R.color.text_secondary),
        BACKGROUND_SECONDARY(true, R.color.block_background, R.color.text_secondary),
        NO_BACKGROUND(false, null, R.color.text_secondary)
    }

    private val binder = ViewPlaceholderBinding.inflate(inflater(), this)

    init {
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

        val imageTint = typedArray.getColorOrNull(R.styleable.PlaceholderView_imageTint)
        imageTint?.let(::setImageTint)

        val showButton = typedArray.getBoolean(R.styleable.PlaceholderView_showButton, true)
        binder.viewPlaceholderButton.isVisible = showButton
    }

    fun setStyle(style: Style) {
        background = if (style.showBackground) {
            context.getRoundedCornerDrawable(style.backgroundColorRes!!, cornerSizeInDp = 12)
        } else {
            null
        }
        binder.viewPlaceholderText.setTextColorRes(style.textColorRes)
    }

    fun setImage(@DrawableRes image: Int) {
        binder.viewPlaceholderImage.setImageResource(image)
    }

    fun setImageTint(@ColorInt tint: Int?) {
        binder.viewPlaceholderImage.setImageTint(tint)
    }

    fun setText(text: String) {
        binder.viewPlaceholderText.text = text
    }

    fun setText(@StringRes textRes: Int) {
        binder.viewPlaceholderText.setText(textRes)
    }

    fun setButtonText(text: String?) {
        binder.viewPlaceholderButton.setTextOrHide(text)
    }

    fun setButtonText(@StringRes textRes: Int) {
        setButtonText(context.getString(textRes))
    }

    fun setModel(model: PlaceholderModel) {
        setText(model.text)
        setImage(model.imageRes)
        setButtonText(model.buttonText)
        setImageTint(model.imageTint)
        model.style?.let { setStyle(it) }
    }

    fun setButtonClickListener(listener: OnClickListener?) {
        binder.viewPlaceholderButton.setOnClickListener(listener)
    }
}

class PlaceholderModel(
    val text: String,
    @DrawableRes val imageRes: Int,
    val buttonText: String? = null,
    val style: PlaceholderView.Style? = null,
    @ColorInt val imageTint: Int? = null
)

fun PlaceholderView.setModelOrHide(model: PlaceholderModel?) {
    model?.let { setModel(it) }
    isVisible = model != null
}
