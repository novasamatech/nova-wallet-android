package io.novafoundation.nova.common.view.tabs

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.widget.CompoundButton
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getCornersCheckableDrawable
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable

class TabItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CompoundButton(ContextThemeWrapper(context, R.style.Widget_Nova_TabItem), attrs, defStyleAttr) {

    init {
        background = with(context) {
            getCornersCheckableDrawable(
                checked = addRipple(getRoundedCornerDrawable(fillColorRes = R.color.white_16)),
                unchecked = addRipple(
                    drawable = getRoundedCornerDrawable(fillColorRes = android.R.color.transparent),
                    mask = getRoundedCornerDrawable(fillColorRes = R.color.black_48)
                )
            )
        }
    }
}
