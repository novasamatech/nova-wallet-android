package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewAccentActionBinding
import io.novafoundation.nova.common.utils.inflater

class AccentActionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binder = ViewAccentActionBinding.inflate(inflater(), this)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        setBackgroundResource(R.drawable.bg_primary_list_item)
    }

    fun setText(@StringRes textRes: Int) {
        binder.accentActionText.setText(textRes)
    }

    fun setIcon(@DrawableRes iconRes: Int) {
        binder.accentActionIcon.setImageResource(iconRes)
    }
}
