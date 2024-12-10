package io.novafoundation.nova.common.view.settings

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.isVisible
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewSettingsItemBinding
import io.novafoundation.nova.common.utils.getDrawableCompat
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.useAttributes

class SettingsItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binder = ViewSettingsItemBinding.inflate(inflater(), this)

    init {
        orientation = HORIZONTAL
        background = context.getDrawableCompat(R.drawable.bg_primary_list_item)

        attrs?.let(::applyAttributes)
    }

    fun setTitle(title: String?) {
        binder.settingsItemTitle.text = title
    }

    fun setValue(value: String?) {
        binder.settingsItemValue.text = value
    }

    fun setIcon(icon: Drawable?) {
        binder.settingsItemIcon.isVisible = icon != null
        binder.settingsItemIcon.setImageDrawable(icon)
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
