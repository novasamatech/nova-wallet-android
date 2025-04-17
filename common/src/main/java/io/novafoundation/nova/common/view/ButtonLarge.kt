package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ButtonLargeBinding
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.getColorFromAttr
import io.novafoundation.nova.common.utils.getEnum
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawableFromColors

class ButtonLarge @kotlin.jvm.JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    private val binder = ButtonLargeBinding.inflate(inflater(), this)

    init {
        minHeight = 52.dp

        attrs?.let(::applyAttributes)
    }

    enum class Style {
        PRIMARY,
        SECONDARY,
    }

    fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.ButtonLarge) {
        val style = it.getEnum(R.styleable.ButtonLarge_buttonLargeStyle, Style.PRIMARY)
        setStyle(style)

        val icon = it.getDrawable(R.styleable.ButtonLarge_icon)
        setIcon(icon)

        val title = it.getString(R.styleable.ButtonLarge_title)
        setTitle(title)

        val subtitle = it.getString(R.styleable.ButtonLarge_subTitle)
        setSubtitle(subtitle)
    }

    private fun setTitle(title: String?) {
        binder.buttonLargeTitle.text = title
    }

    private fun setSubtitle(subtitle: String?) {
        binder.buttonLargeSubtitle.setTextOrHide(subtitle)
    }

    private fun setIcon(icon: Drawable?) {
        binder.buttonLargeIcon.setImageDrawable(icon)
    }

    private fun setStyle(style: Style) = with(context) {
        val backgroundColor = when (style) {
            Style.PRIMARY -> context.getColor(R.color.button_background_primary)
            Style.SECONDARY -> context.getColor(R.color.button_background_secondary)
        }

        val rippleColor = getColorFromAttr(R.attr.colorControlHighlight)
        val baseBackground = context.getRoundedCornerDrawableFromColors(backgroundColor)

        background = addRipple(baseBackground, mask = null, rippleColor = rippleColor)
    }
}
