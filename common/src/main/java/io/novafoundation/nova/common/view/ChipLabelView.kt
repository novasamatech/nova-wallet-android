package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.Gravity
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatTextView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable

class ChipLabelModel(
    @DrawableRes val iconRes: Int,
    val title: String
)

class ChipLabelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(ContextThemeWrapper(context, R.style.Widget_Nova_ChipLabel), attrs, defStyleAttr) {

    init {
        background = context.getRoundedCornerDrawable(R.color.chips_background, cornerSizeInDp = 8)
        gravity = Gravity.CENTER_VERTICAL
    }

    fun setModel(model: ChipLabelModel) {
        setDrawableStart(model.iconRes, widthInDp = 16, paddingInDp = 6, tint = R.color.icon_secondary)
        text = model.title
    }
}
