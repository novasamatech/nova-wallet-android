package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import coil.ImageLoader
import coil.load
import coil.transform.RoundedCornersTransformation
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setDrawableEnd
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

open class TableCellView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

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

    fun loadImage(url: String?) {
        url?.let {
            image.makeVisible()
            image.load(url, imageLoader) {
                transformations(RoundedCornersTransformation(10.dpF(context)))
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

    fun showValue(primary: String, secondary: String? = null) {
        contentGroup.makeVisible()

        valuePrimary.text = primary
        valueSecondary.setTextOrHide(secondary)

        valueProgress.makeGone()
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
            val primaryValueIconTint = typedArray.getResourceId(R.styleable.TableCellView_primaryValueIconTint, R.color.white_64)

            setPrimaryValueIcon(primaryValueIcon, primaryValueIconTint)
        }
    }
}
