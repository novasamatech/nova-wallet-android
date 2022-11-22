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
                checked = addRipple(getRoundedCornerDrawable(fillColorRes = R.color.segmented_tab_active, cornerSizeInDp = 10)),
                unchecked = addRipple(
                    drawable = getRoundedCornerDrawable(fillColorRes = android.R.color.transparent, cornerSizeInDp = 10),
                    mask = getRoundedCornerDrawable(fillColorRes = R.color.cell_background_pressed, cornerSizeInDp = 10)
                )
            )
        }
    }
}
