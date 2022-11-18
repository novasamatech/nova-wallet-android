package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.core.content.res.getDimensionOrThrow
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.android.synthetic.main.view_chip.view.chipIcon
import kotlinx.android.synthetic.main.view_chip.view.chipText
import kotlin.math.roundToInt

class NovaChipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    init {
        View.inflate(context, R.layout.view_chip, this)
        orientation = HORIZONTAL

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.NovaChipView)

        if (typedArray.hasValue(R.styleable.NovaChipView_chipIcon)) {
            val iconDrawable = typedArray.getDrawable(R.styleable.NovaChipView_chipIcon)
            setIcon(iconDrawable)
        } else {
            setIcon(null)
        }

        if (typedArray.hasValue(R.styleable.NovaChipView_chipIconSize)) {
            val iconSize = typedArray.getDimensionOrThrow(R.styleable.NovaChipView_chipIconSize)
            setIconSize(iconSize)
        }

        val backgroundTintColor = typedArray.getResourceId(R.styleable.NovaChipView_backgroundColor, R.color.chips_background)
        background = getRoundedCornerDrawable(backgroundTintColor, cornerSizeDp = 8)
            .withRippleMask(getRippleMask(cornerSizeDp = 8))

        val textAppearanceId = typedArray.getResourceId(
            R.styleable.NovaChipView_chipTextAppearance,
            R.style.TextAppearance_NovaFoundation_SemiBold_Caps1
        )
        chipText.setTextAppearance(textAppearanceId)

        val text = typedArray.getString(R.styleable.NovaChipView_android_text)
        setText(text)

        @ColorRes
        val textColorRes = typedArray.getResourceId(
            R.styleable.NovaChipView_android_textColor,
            R.color.chip_text
        )
        chipText.setTextColorRes(textColorRes)

        typedArray.recycle()
    }

    fun setIconSize(value: Float) {
        chipIcon.updateLayoutParams<LayoutParams> {
            val intValue = value.roundToInt()
            this.height = intValue
            this.width = intValue
        }
    }

    fun setIcon(icon: Icon?, imageLoader: ImageLoader) {
        if (icon == null) {
            setIcon(null)
        } else {
            chipIcon.setIcon(icon, imageLoader)
        }
        useIcon(icon != null)
    }

    fun setIcon(drawable: Drawable?) {
        chipIcon.setImageDrawable(drawable)
        useIcon(drawable != null)
    }

    fun setText(text: String?) {
        chipText.text = text
    }

    private fun useIcon(useIcon: Boolean) {
        chipIcon.isVisible = useIcon

        val startPadding = if (useIcon) 6 else 8
        updatePadding(start = startPadding.dp)
    }

    fun clearIcon() {
        chipIcon.clear()
    }
}
