package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.useAttributes
import kotlinx.android.synthetic.main.view_switch.view.viewSwitchField
import kotlinx.android.synthetic.main.view_switch.view.viewSwitchSubtitle
import kotlinx.android.synthetic.main.view_switch.view.viewSwitchTitle

private const val DIVIDER_VISIBLE_DEFAULT = false

class Switch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    val field: CompoundButton
        get() = viewSwitchField

    init {
        View.inflate(context, R.layout.view_switch, this)

        attrs?.let(::applyAttributes)
    }

    fun setTitle(title: String) {
        viewSwitchTitle.text = title
    }

    fun setSubtitle(subtitle: String?) {
        viewSwitchSubtitle.setTextOrHide(subtitle)
    }

    fun setDividerVisible(dividerVisible: Boolean) {
        if (dividerVisible) {
            setBackgroundResource(R.drawable.divider_drawable)
        } else {
            background = null
        }
    }

    private fun applyAttributes(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.Switch) {
        val title = it.getString(R.styleable.Switch_title)
        title?.let(::setTitle)

        val subtitle = it.getString(R.styleable.Switch_subtitle)
        setSubtitle(subtitle)

        val dividerVisible = it.getBoolean(R.styleable.Switch_dividerVisible, DIVIDER_VISIBLE_DEFAULT)
        setDividerVisible(dividerVisible)
    }
}
