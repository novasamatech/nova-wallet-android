package io.novafoundation.nova.common.view.settings

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import io.novafoundation.nova.common.R

class SettingsGroupHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : TextView(context, attrs, defStyleAttr, R.style.Widget_Nova_Text_SettingsHeader)
