package io.novafoundation.nova.common.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.getRippleMask
import io.novafoundation.nova.common.utils.getRoundedCornerDrawable
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.withRippleMask
import kotlinx.android.synthetic.main.view_go_next.view.goNextActionImage
import kotlinx.android.synthetic.main.view_go_next.view.goNextBadgeText
import kotlinx.android.synthetic.main.view_go_next.view.goNextDivider
import kotlinx.android.synthetic.main.view_go_next.view.goNextIcon
import kotlinx.android.synthetic.main.view_go_next.view.goNextProgress
import kotlinx.android.synthetic.main.view_go_next.view.goNextTitle

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
