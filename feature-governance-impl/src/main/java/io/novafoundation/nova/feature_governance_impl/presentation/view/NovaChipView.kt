package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.text.TextUtils.TruncateAt
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
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.getEnum
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.android.synthetic.main.view_chip.view.chipDrawablePadding
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
        val drawablePadding: Float,
        val iconVerticalMargin: Float,
        val iconHorizontalMargin: Float,
        val textTopMargin: Float,
        val textBottomMargin: Float,
        val textHorizontalMargin: Float,
        @StyleRes val textAppearanceRes: Int,
        val cornerRadiusDp: Int
    ) {
        NORMAL(
            drawablePadding = 6f,
            iconVerticalMargin = 3.0f,
            iconHorizontalMargin = 6.0f,
            textTopMargin = 4.5f,
            textBottomMargin = 4.5f,
            textHorizontalMargin = 8.0f,
            textAppearanceRes = R.style.TextAppearance_NovaFoundation_SemiBold_Caps1,
            cornerRadiusDp = 8
        ),

        SMALL(
            drawablePadding = 4f,
            iconVerticalMargin = 1.5f,
            iconHorizontalMargin = 4.0f,
            textTopMargin = 1.5f,
            textBottomMargin = 1.5f,
            textHorizontalMargin = 6.0f,
            textAppearanceRes = R.style.TextAppearance_NovaFoundation_SemiBold_Caps2,
            cornerRadiusDp = 6
        ),

        SUM(
            drawablePadding = 0f,
            iconVerticalMargin = 0f,
            iconHorizontalMargin = 0f,
            textTopMargin = 3f,
            textBottomMargin = 3f,
            textHorizontalMargin = 8.0f,
            textAppearanceRes = R.style.TextAppearance_NovaFoundation_SemiBold_Footnote,
            cornerRadiusDp = 8
        )
    }

    private var size: Size = SIZE_DEFAULT

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        if (isInEditMode) {
            ImageLoader.invoke(context)
        } else {
            FeatureUtils.getCommonApi(context).imageLoader()
        }
    }

    private val customTextAppearance: Int?

    init {
        View.inflate(context, R.layout.view_chip, this)
        orientation = HORIZONTAL

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.NovaChipView)

        val size = typedArray.getEnum(R.styleable.NovaChipView_chipSize, SIZE_DEFAULT)

        customTextAppearance = typedArray.getResourceIdOrNull(R.styleable.NovaChipView_chipTextAppearance)
        setSize(size, customTextAppearance)

        if (typedArray.hasValue(R.styleable.NovaChipView_chipIcon)) {
            val iconDrawable = typedArray.getDrawable(R.styleable.NovaChipView_chipIcon)
            setIconDrawable(iconDrawable)
        } else {
            setIconDrawable(null)
        }

        if (typedArray.hasValue(R.styleable.NovaChipView_chipIconSize)) {
            val iconSize = typedArray.getDimensionOrThrow(R.styleable.NovaChipView_chipIconSize)
            setIconSize(iconSize)
        }

        val backgroundTintColor = typedArray.getResourceId(R.styleable.NovaChipView_backgroundColor, R.color.chips_background)
        setChipBackground(backgroundTintColor)

        val text = typedArray.getString(R.styleable.NovaChipView_android_text)
        setText(text)

        val textAllCaps = typedArray.getBoolean(R.styleable.NovaChipView_android_textAllCaps, true)
        setTextAllCaps(textAllCaps)

        @ColorRes
        val textColorRes = typedArray.getResourceId(
            R.styleable.NovaChipView_android_textColor,
            R.color.chip_text
        )
        chipText.setTextColorRes(textColorRes)

        chipText.ellipsize = typedArray.getEllipsize()

        typedArray.recycle()
    }

    fun TypedArray.getEllipsize(): TruncateAt? {
        val index = getInt(R.styleable.NovaChipView_android_ellipsize, -1)

        if (index <= 0) return null
        return TruncateAt.values()[index - 1]
    }

    fun setSize(size: Size, customTextAppearance: Int? = null) {
        this.size = size

        val startPadding = if (chipIcon.isVisible) {
            size.iconHorizontalMargin.dp
        } else {
            size.textHorizontalMargin.dp
        }

        val endPadding = if (chipText.isVisible) {
            size.textHorizontalMargin.dp
        } else {
            size.iconHorizontalMargin.dp
        }

        updatePadding(start = startPadding, end = endPadding)

        chipIcon.updateLayoutParams<MarginLayoutParams> {
            val vertical = size.iconVerticalMargin.dp
            setMargins(0, vertical, 0, vertical)
        }

        chipText.updateLayoutParams<MarginLayoutParams> {
            val top = size.textTopMargin.dp
            val bottom = size.textBottomMargin.dp

            setMargins(0, top, 0, bottom)
        }

        chipText.setTextAppearance(customTextAppearance ?: size.textAppearanceRes)

        chipDrawablePadding.layoutParams = LayoutParams(size.drawablePadding.dp, LayoutParams.MATCH_PARENT)
    }

    fun setIconSize(value: Float) {
        chipIcon.updateLayoutParams<LayoutParams> {
            val intValue = value.roundToInt()
            this.height = intValue
            this.width = intValue
        }
    }

    fun setIconTint(tintRes: Int?) {
        chipIcon.setImageTintRes(tintRes)
    }

    fun setIcon(icon: Icon?) {
        if (icon == null) {
            setIconDrawable(drawable = null)
        } else {
            chipIcon.setIcon(icon, imageLoader)
        }
        useIcon(icon != null)
        invalidateDrawablePadding()
    }

    fun setIconDrawable(drawable: Drawable?) {
        chipIcon.setImageDrawable(drawable)
        useIcon(drawable != null)
        invalidateDrawablePadding()
    }

    fun setIcon(@DrawableRes drawableRes: Int) {
        chipIcon.setImageResource(drawableRes)
        useIcon(true)
        invalidateDrawablePadding()
    }

    fun setStyle(@ColorRes backgroundColorRes: Int, @ColorRes textColorRes: Int, @ColorRes iconColorRes: Int) {
        setChipBackground(backgroundColorRes)
        chipText.setTextColorRes(textColorRes)
        setIconTint(textColorRes)
    }

    fun setText(text: CharSequence?) {
        chipText.setTextOrHide(text)
        invalidateDrawablePadding()
    }

    fun setTextAllCaps(value: Boolean) {
        chipText.isAllCaps = value
    }

    private fun useIcon(useIcon: Boolean) {
        chipIcon.isVisible = useIcon

        refreshSize()
    }

    fun clearIcon() {
        chipIcon.clear()
    }

    private fun invalidateDrawablePadding() {
        chipDrawablePadding.isVisible = chipIcon.isVisible && chipText.isVisible
    }

    private fun setChipBackground(backgroundTintColor: Int) {
        background = getRoundedCornerDrawable(backgroundTintColor, cornerSizeDp = size.cornerRadiusDp)
            .withRippleMask(getRippleMask(cornerSizeDp = size.cornerRadiusDp))
    }

    private fun refreshSize() {
        setSize(size, customTextAppearance)
    }
}

fun NovaChipView.setTextOrHide(text: CharSequence?) = letOrHide(text, ::setText)
