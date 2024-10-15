package io.novafoundation.nova.common.view.settings

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewSettingsSwitcherBinding
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setCompoundDrawableTintRes
import io.novafoundation.nova.common.utils.useAttributes

class SettingsSwitcherView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binder = ViewSettingsSwitcherBinding.inflate(inflater(), this)

    init {
        View.inflate(context, R.layout.view_settings_switcher, this)

        attrs?.let(::applyAttributes)
    }

    fun setTitle(title: String?) {
        binder.settingsSwitcher.text = title
    }

    fun setIconTintColor(@ColorRes tintRes: Int?) {
        binder.settingsSwitcher.setCompoundDrawableTintRes(tintRes)
    }

    fun setIcon(icon: Drawable?) {
        // Set icon size 24 dp
        val iconSize = 24.dp
        icon?.setBounds(0, 0, iconSize, iconSize)
        binder.settingsSwitcher.setCompoundDrawables(icon, null, null, null)
    }

    fun setChecked(checked: Boolean) {
        binder.settingsSwitcher.isChecked = checked
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        binder.settingsSwitcher.isEnabled = enabled
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.SettingsSwitcherView) {
        val title = it.getString(R.styleable.SettingsSwitcherView_title)
        setTitle(title)

        val icon = it.getDrawable(R.styleable.SettingsSwitcherView_icon)
        setIcon(icon)

        val textColorStateList = it.getColorStateList(R.styleable.SettingsSwitcherView_switcherTextColor)
        textColorStateList?.let { binder.settingsSwitcher.setTextColor(it) }
    }
}
