package io.novafoundation.nova.feature_settings_impl.presentation.settings.view

import android.content.Context
import android.util.AttributeSet
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.getDrawableCompat
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_settings_impl.R

class SettingsGroupView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = true

        orientation = VERTICAL

        background = context.getRoundedCornerDrawable(fillColorRes = R.color.block_background)

        dividerDrawable = context.getDrawableCompat(R.drawable.divider_decoration)
        showDividers = SHOW_DIVIDER_MIDDLE
    }
}
