package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.R

class AccentActionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_accent_action, this)

        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        setBackgroundResource(R.drawable.bg_primary_list_item)
    }

    fun setText(@StringRes textRes: Int) {
        accentActionText.setText(textRes)
    }

    fun setIcon(@DrawableRes iconRes: Int) {
        accentActionIcon.setImageResource(iconRes)
    }
}
