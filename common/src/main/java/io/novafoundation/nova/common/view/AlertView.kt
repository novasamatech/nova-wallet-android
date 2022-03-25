package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
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

    enum class Style(@DrawableRes val iconRes: Int, val backgroundColorRes: Int) {
        WARNING(R.drawable.ic_warning_filled, R.color.yellow_12)
    }

    init {
        View.inflate(context, R.layout.view_alert, this)

        orientation = HORIZONTAL

        updatePadding(top = 10.dp, start = 16.dp, end = 16.dp, bottom = 10.dp)

        attrs?.let(::applyAttrs)
    }

    fun setStyle(style: Style) {
        background = getRoundedCornerDrawable(fillColorRes = style.backgroundColorRes)
        alertIcon.setImageResource(style.iconRes)
    }

    fun setText(text: String) {
        alertMessage.text = text
    }

    private fun applyAttrs(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.AlertView) {
        val style = it.getEnum(R.styleable.AlertView_style, Style.WARNING)
        setStyle(style)

        val text = it.getString(R.styleable.AlertView_android_text)
        text?.let(::setText)
    }
}
