package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.setPadding
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.getRippleMask
import io.novafoundation.nova.common.utils.getRoundedCornerDrawable
import io.novafoundation.nova.common.utils.withRippleMask

class YourWalletsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_your_wallets, this)

        setPadding(4.dp)
        background = getRoundedCornerDrawable(cornerSizeDp = 8).withRippleMask(getRippleMask(cornerSizeDp = 8))
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
}
