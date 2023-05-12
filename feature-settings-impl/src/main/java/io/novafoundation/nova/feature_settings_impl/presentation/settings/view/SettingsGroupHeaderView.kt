package io.novafoundation.nova.feature_settings_impl.presentation.settings.view

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import io.novafoundation.nova.feature_settings_impl.R

class SettingsGroupHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : TextView(context, attrs, defStyleAttr, R.style.Widget_Nova_Text_SettingsHeader)
