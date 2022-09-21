package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.content.res.getColorOrThrow
import coil.ImageLoader
import coil.load
import coil.transform.RoundedCornersTransformation
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.getAccentColor
import io.novafoundation.nova.common.utils.getEnum
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.postToSelf
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.useAttributes
import kotlinx.android.synthetic.main.view_table_cell.view.tableCellContent
import kotlinx.android.synthetic.main.view_table_cell.view.tableCellImage
import kotlinx.android.synthetic.main.view_table_cell.view.tableCellTitle
import kotlinx.android.synthetic.main.view_table_cell.view.tableCellValueDivider
import kotlinx.android.synthetic.main.view_table_cell.view.tableCellValuePrimary
import kotlinx.android.synthetic.main.view_table_cell.view.tableCellValueProgress
import kotlinx.android.synthetic.main.view_table_cell.view.tableCellValueSecondary

private val ICON_TINT_DEFAULT = R.color.white_64

open class TableCellView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0,
) : ConstraintLayout(context, attrs, defStyle, defStyleRes) {

    enum class FieldStyle {
        TEXT, LINK
    }

    companion object {
        fun createTableCellView(context: Context): TableCellView {
            return TableCellView(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

                setDividerColor(R.color.white_24)

                valueSecondary.setTextColorRes(R.color.white_64)
                title.setTextColorRes(R.color.white_64)
            }
        }
    }

    val title: TextView
        get() = tableCellTitle

    val valuePrimary: TextView
        get() = tableCellValuePrimary

    val valueSecondary: TextView
        get() = tableCellValueSecondary

    val image: ImageView
        get() = tableCellImage

    private val valueProgress: ProgressBar
        get() = tableCellValueProgress

    private val contentGroup: Group
        get() = tableCellContent

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        FeatureUtils.getCommonApi(context).imageLoader()
    }

    init {
        View.inflate(context, R.layout.view_table_cell, this)

        setBackgroundResource(R.drawable.bg_primary_list_item)

        attrs?.let { applyAttributes(it) }
    }

    fun setTitle(titleRes: Int) {
        tableCellTitle.setText(titleRes)
    }

    fun setTitle(title: String?) {
        tableCellTitle.text = title
    }

    fun setImage(src: Drawable) {
        image.setImageDrawable(src)
        image.makeVisible()
    }

    fun loadImage(
        url: String?,
        @DrawableRes placeholderRes: Int? = null,
        roundedCornersDp: Int? = 10
    ) {
        url?.let {
            image.makeVisible()
            image.load(url, imageLoader) {
                roundedCornersDp?.let {
                    transformations(RoundedCornersTransformation(roundedCornersDp.dpF(context)))
                }

                placeholderRes?.let {
                    placeholder(it)
                    error(it)
                }
            }
        }
    }

    fun showProgress() {
        contentGroup.makeGone()
        valueProgress.makeVisible()
    }

    fun setDividerVisible(visible: Boolean) {
        tableCellValueDivider.setVisible(visible)
    }

    fun setDividerColor(@ColorRes colorRes: Int) {
        tableCellValueDivider.setBackgroundColor(context.getColor(colorRes))
    }

    fun setPrimaryValueIcon(@DrawableRes icon: Int, @ColorRes tint: Int? = null) {
        tableCellValuePrimary.setDrawableEnd(icon, widthInDp = 16, paddingInDp = 8, tint = tint)
    }

    fun setPrimaryValueStyle(style: FieldStyle) {
        when (style) {
            FieldStyle.TEXT -> {
                valuePrimary.setTextColorRes(R.color.white)
            }
            FieldStyle.LINK -> {
                valuePrimary.setTextColor(context.getAccentColor())
            }
        }
    }

    fun setTitleIcon(@DrawableRes icon: Int?) {
        tableCellTitle.setDrawableEnd(icon, widthInDp = 16, paddingInDp = 4, tint = ICON_TINT_DEFAULT)
    }

    fun setTitleTextColor(@ColorInt color: Int) {
        title.setTextColor(color)
    }

    fun setPrimaryValueTextColor(@ColorInt color: Int) {
        valuePrimary.setTextColor(color)
    }

    fun setSecondaryValueTextColor(@ColorInt color: Int) {
        valueSecondary.setTextColor(color)
    }

    fun setTitleSize(size: Float) {
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
    }

    fun setPrimaryValueTextSize(size: Float) {
        valuePrimary.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
    }

    fun setSecondaryValueTextSize(size: Float) {
        valueSecondary.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
    }

    fun showValue(primary: String, secondary: String? = null) {
        postToSelf {
            contentGroup.makeVisible()

            valuePrimary.text = primary
            valueSecondary.setTextOrHide(secondary)

            valueProgress.makeGone()
        }
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.TableCellView) { typedArray ->
        val title = typedArray.getString(R.styleable.TableCellView_title)
        setTitle(title)

        val dividerVisible = typedArray.getBoolean(R.styleable.TableCellView_dividerVisible, true)
        setDividerVisible(dividerVisible)

        val dividerColor = typedArray.getResourceIdOrNull(R.styleable.TableCellView_dividerColor)
        dividerColor?.let(::setDividerColor)

        val primaryValueIcon = typedArray.getResourceIdOrNull(R.styleable.TableCellView_primaryValueIcon)
        primaryValueIcon?.let {
            val primaryValueIconTint = typedArray.getResourceId(R.styleable.TableCellView_primaryValueIconTint, ICON_TINT_DEFAULT)

            setPrimaryValueIcon(primaryValueIcon, primaryValueIconTint)
        }

        val primaryValueStyle = typedArray.getEnum(R.styleable.TableCellView_primaryValueStyle, default = FieldStyle.TEXT)
        setPrimaryValueStyle(primaryValueStyle)

        val titleIcon = typedArray.getResourceIdOrNull(R.styleable.TableCellView_titleIcon)
        titleIcon?.let(::setTitleIcon)

        val primaryValueTextSize = typedArray.getDimension(R.styleable.TableCellView_primaryValueTextSize, valuePrimary.textSize)
        setPrimaryValueTextSize(primaryValueTextSize)

        val secondaryValueTextSize = typedArray.getDimension(R.styleable.TableCellView_secondaryValueTextSize, valueSecondary.textSize)
        setSecondaryValueTextSize(secondaryValueTextSize)

        val titleSize = typedArray.getDimension(R.styleable.TableCellView_titleSize, valuePrimary.textSize)
        setTitleSize(titleSize)

        try {
            val titleTextColor = typedArray.getColorOrThrow(R.styleable.TableCellView_titleTextColor)
            setTitleTextColor(titleTextColor)
        } finally {
        }

        try {
            val primaryValueTextColor = typedArray.getColorOrThrow(R.styleable.TableCellView_primaryValueTextColor)
            setPrimaryValueTextColor(primaryValueTextColor)
        } finally {
        }

        try {
            val secondaryValueTextColor = typedArray.getColorOrThrow(R.styleable.TableCellView_secondaryValueTextColor)
            setSecondaryValueTextColor(secondaryValueTextColor)
        } finally {
        }
    }
}

fun TableCellView.showValueOrHide(primary: String?, secondary: String? = null) {
    if (primary != null) {
        showValue(primary, secondary)
    }

    setVisible(primary != null)
}
