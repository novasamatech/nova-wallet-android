package io.novafoundation.nova.feature_settings_impl.presentation.settings.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_settings_impl.R
import kotlinx.android.synthetic.main.view_settings_switcher.view.settingsSwitcher

class SettingsSwitcherView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_settings_switcher, this)

        attrs?.let(::applyAttributes)
    }

    fun setTitle(title: String?) {
        settingsSwitcher.text = title
    }

    fun setIcon(icon: Drawable?) {
        settingsSwitcher.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
    }

    fun setChecked(checked: Boolean) {
        settingsSwitcher.isChecked = checked
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.SettingsSwitcherView) {
        val title = it.getString(R.styleable.SettingsSwitcherView_title)
        setTitle(title)

        val icon = it.getDrawable(R.styleable.SettingsSwitcherView_icon)
        setIcon(icon)
    }
}
