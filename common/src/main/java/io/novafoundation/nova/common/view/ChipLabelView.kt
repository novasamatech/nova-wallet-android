package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.Gravity
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatTextView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.px
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import kotlin.math.roundToInt

private const val BASE_ICON_PADDING_DP = 6

class ChipLabelModel(
    @DrawableRes val iconRes: Int,
    val title: String
)

class ChipLabelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(ContextThemeWrapper(context, R.style.Widget_Nova_ChipLabel), attrs, defStyleAttr) {

    private var startIconTint: Int = R.color.chip_icon
    private var iconStartPadding: Float = 0f
    private var endIconTint: Int = R.color.chip_icon
    private var iconEndPadding: Float = 0f

    init {
        background = context.getRoundedCornerDrawable(R.color.chips_background, cornerSizeInDp = 8)
        gravity = Gravity.CENTER_VERTICAL

        applyAttrs(context, attrs)
    }

    fun setModel(model: ChipLabelModel) {
        setDrawableStart(model.iconRes, widthInDp = 16, paddingInDp = 6, tint = R.color.icon_secondary)
        text = model.title
    }

    private fun applyAttrs(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ChipLabelView)

        iconStartPadding = typedArray.getDimension(R.styleable.ChipLabelView_iconStartPadding, BASE_ICON_PADDING_DP.dpF(context))
        startIconTint = typedArray.getResourceId(R.styleable.ChipLabelView_iconStartTint, R.color.chip_icon)
        typedArray.getResourceIdOrNull(R.styleable.ChipLabelView_iconStart)?.let {
            setDrawableStart(it, widthInDp = 16, paddingInDp = iconStartPadding.roundToInt(), tint = startIconTint)
        }

        iconEndPadding = typedArray.getDimension(R.styleable.ChipLabelView_iconEndPadding, BASE_ICON_PADDING_DP.dpF(context))
        endIconTint = typedArray.getResourceId(R.styleable.ChipLabelView_iconEndTint, R.color.chip_icon)
        typedArray.getResourceIdOrNull(R.styleable.ChipLabelView_iconEnd)?.let {
            setDrawableEnd(it, widthInDp = 16, paddingInDp = iconEndPadding.px(context), tint = endIconTint)
        }

        typedArray.recycle()
    }
}
