package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.getEnum
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.common.utils.useAttributes
import kotlinx.android.synthetic.main.view_alert.view.alertIcon
import kotlinx.android.synthetic.main.view_alert.view.alertMessage

class AlertView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    enum class StylePreset {
        WARNING, ERROR
    }

    class Style(@DrawableRes val iconRes: Int, @ColorRes val backgroundColorRes: Int)

    init {
        View.inflate(context, R.layout.view_alert, this)

        orientation = HORIZONTAL

        updatePadding(top = 10.dp, start = 16.dp, end = 16.dp, bottom = 10.dp)

        attrs?.let(::applyAttrs)
    }

    fun setStyle(style: Style) {
        setStyleBackground(style.backgroundColorRes)
        setStyleIcon(style.iconRes)
    }

    fun setStylePreset(preset: StylePreset) {
        setStyle(styleFromPreset(preset))
    }

    fun setText(text: String) {
        alertMessage.text = text
    }

    fun setText(@StringRes textRes: Int) {
        alertMessage.setText(textRes)
    }

    private fun setStyleBackground(@ColorRes colorRes: Int) {
        background = getRoundedCornerDrawable(fillColorRes = colorRes)
    }

    private fun setStyleIcon(@DrawableRes iconRes: Int) {
        alertIcon.setImageResource(iconRes)
    }

    private fun applyAttrs(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.AlertView) {
        val stylePreset = it.getEnum(R.styleable.AlertView_style, StylePreset.WARNING)
        val styleFromPreset = styleFromPreset(stylePreset)

        val backgroundColorRes = it.getResourceId(R.styleable.AlertView_styleBackgroundColor, styleFromPreset.backgroundColorRes)
        val iconRes = it.getResourceId(R.styleable.AlertView_styleIcon, styleFromPreset.iconRes)

        setStyle(Style(iconRes, backgroundColorRes))

        val text = it.getString(R.styleable.AlertView_android_text)
        text?.let(::setText)
    }

    private fun styleFromPreset(preset: StylePreset) = when (preset) {
        StylePreset.WARNING -> Style(R.drawable.ic_warning_filled, R.color.warning_block_background)
        StylePreset.ERROR -> Style(R.drawable.ic_slash, R.color.error_block_background)
    }
}
