package io.novafoundation.nova.common.view.settings

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.setCompoundDrawableTint
import io.novafoundation.nova.common.utils.useAttributes
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

    fun setIconTintColor(@ColorRes tintRes: Int?) {
        settingsSwitcher.setCompoundDrawableTint(tintRes)
    }

    fun setIcon(icon: Drawable?) {
        // Set icon size 24 dp
        val iconSize = 24.dp
        icon?.setBounds(0, 0, iconSize, iconSize)
        settingsSwitcher.setCompoundDrawables(icon, null, null, null)
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
