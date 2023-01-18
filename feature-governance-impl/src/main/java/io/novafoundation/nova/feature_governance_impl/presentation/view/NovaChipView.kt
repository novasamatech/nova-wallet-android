package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.content.res.getDimensionOrThrow
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.getEnum
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.android.synthetic.main.view_chip.view.chipIcon
import kotlinx.android.synthetic.main.view_chip.view.chipText
import kotlin.math.roundToInt

private val SIZE_DEFAULT = NovaChipView.Size.NORMAL

class NovaChipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    enum class Size(
        val iconVerticalMargin: Float,
        val iconHorizontalMargin: Float,
        val textVerticalMargin: Float,
        val textHorizontalMargin: Float,
        @StyleRes val textAppearanceRes: Int
    ) {
        NORMAL(
            iconVerticalMargin = 3.0f,
            iconHorizontalMargin = 6.0f,
            textVerticalMargin = 4.5f,
            textHorizontalMargin = 8.0f,
            textAppearanceRes = R.style.TextAppearance_NovaFoundation_SemiBold_Caps1
        ),

        SMALL(
            iconVerticalMargin = 1.5f,
            iconHorizontalMargin = 4.0f,
            textVerticalMargin = 1.5f,
            textHorizontalMargin = 6.0f,
            textAppearanceRes = R.style.TextAppearance_NovaFoundation_SemiBold_Caps2
        )
    }

    private var size: Size = SIZE_DEFAULT

    init {
        View.inflate(context, R.layout.view_chip, this)
        orientation = HORIZONTAL

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.NovaChipView)

        val size = typedArray.getEnum(R.styleable.NovaChipView_chipSize, SIZE_DEFAULT)
        setSize(size)

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
        setChipBackground(backgroundTintColor)

        val textAppearanceId = typedArray.getResourceIdOrNull(R.styleable.NovaChipView_chipTextAppearance)
        textAppearanceId?.let(chipText::setTextAppearance)

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

    fun setSize(size: Size) {
        this.size = size

        val startPadding = if (chipIcon.isVisible) {
            size.iconHorizontalMargin.dp
        } else {
            size.textHorizontalMargin.dp
        }
        updatePadding(start = startPadding)

        chipIcon.updateLayoutParams<MarginLayoutParams> {
            val vertical = size.iconVerticalMargin.dp
            val horizontal = size.iconHorizontalMargin.dp
            setMargins(0, vertical, horizontal, vertical)
        }

        chipText.updateLayoutParams<MarginLayoutParams> {
            val vertical = size.textVerticalMargin.dp
            val end = size.textHorizontalMargin.dp

            setMargins(0, vertical, end, vertical)
        }

        chipText.setTextAppearance(size.textAppearanceRes)
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

    fun setIcon(@DrawableRes drawableRes: Int) {
        chipIcon.setImageResource(drawableRes)
        useIcon(true)
    }

    fun setStyle(@ColorRes backgroundColorRes: Int, @ColorRes textColorRes: Int, @ColorRes iconColorRes: Int) {
        setChipBackground(backgroundColorRes)
        chipText.setTextColorRes(textColorRes)
        chipIcon.setImageTintRes(iconColorRes)
    }

    fun setText(text: String?) {
        chipText.text = text
    }

    private fun useIcon(useIcon: Boolean) {
        chipIcon.isVisible = useIcon

        refreshSize()
    }

    fun clearIcon() {
        chipIcon.clear()
    }

    private fun setChipBackground(backgroundTintColor: Int) {
        background = getRoundedCornerDrawable(backgroundTintColor, cornerSizeDp = 8)
            .withRippleMask(getRippleMask(cornerSizeDp = 8))
    }

    private fun refreshSize() {
        setSize(size)
    }
}

fun NovaChipView.setTextOrHide(text: String?) = letOrHide(text, ::setText)
