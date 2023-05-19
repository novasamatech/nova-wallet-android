package io.novafoundation.nova.feature_settings_impl.presentation.settings.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.getDrawableCompat
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_settings_impl.R
import kotlinx.android.synthetic.main.view_settings_item.view.settingsItemIcon
import kotlinx.android.synthetic.main.view_settings_item.view.settingsItemTitle
import kotlinx.android.synthetic.main.view_settings_item.view.settingsItemValue

class SettingsItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_settings_item, this)

        orientation = HORIZONTAL
        background = context.getDrawableCompat(R.drawable.bg_primary_list_item)

        attrs?.let(::applyAttributes)
    }

    fun setTitle(title: String?) {
        settingsItemTitle.text = title
    }

    fun setValue(value: String?) {
        settingsItemValue.text = value
    }

    fun setIcon(icon: Drawable?) {
        settingsItemIcon.setImageDrawable(icon)
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.SettingsItemView) {
        val title = it.getString(R.styleable.SettingsItemView_title)
        setTitle(title)

        val value = it.getString(R.styleable.SettingsItemView_settingValue)
        setValue(value)

        val icon = it.getDrawable(R.styleable.SettingsItemView_icon)
        setIcon(icon)
    }
}
