package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Group
import coil.ImageLoader
import coil.load
import coil.transform.RoundedCornersTransformation
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewTableCellBinding
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.getAccentColor
import io.novafoundation.nova.common.utils.getEnum
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.images.ExtraImageRequestBuilding
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.postToSelf
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.useAttributes

open class TableCellView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0,
) : ConstraintLayout(context, attrs, defStyle, defStyleRes), HasDivider {

    enum class FieldStyle {
        PRIMARY, SECONDARY, LINK, POSITIVE
    }

    companion object {
        fun createTableCellView(context: Context): TableCellView {
            return TableCellView(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

                valueSecondary.setTextColorRes(R.color.text_secondary)
                title.setTextColorRes(R.color.text_secondary)
            }
        }
    }

    private val binder = ViewTableCellBinding.inflate(inflater(), this)

    val title: TextView
        get() = binder.tableCellTitle

    val valuePrimary: TextView
        get() = binder.tableCellValuePrimary

    val valueSecondary: TextView
        get() = binder.tableCellValueSecondary

    val image: ImageView
        get() = binder.tableCellImage

    private val valueProgress: ProgressBar
        get() = binder.tableCellValueProgress

    private val contentGroup: Group
        get() = binder.tableCellContent

    val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        FeatureUtils.getCommonApi(context).imageLoader()
    }

    init {
        View.inflate(context, R.layout.view_table_cell, this)

        setBackgroundResource(R.drawable.bg_primary_list_item)
        minHeight = 44.dp(context)

        attrs?.let { applyAttributes(it) }
    }

    fun setTitle(titleRes: Int) {
        binder.tableCellTitle.setText(titleRes)
    }

    fun setTitle(title: String?) {
        binder.tableCellTitle.text = title
    }

    fun setImage(src: Drawable) {
        image.setImageDrawable(src)
        image.makeVisible()
    }

    fun setImage(@DrawableRes src: Int, sizeDp: Int) {
        image.load(src) {
            size(sizeDp.dp(context))
        }
        image.makeVisible()
    }

    fun setOnValueClickListener(onClick: View.OnClickListener?) {
        valuePrimary.setOnClickListener(onClick)
        valueSecondary.setOnClickListener(onClick)
    }

    fun loadImage(
        url: String?,
        @DrawableRes placeholderRes: Int? = null,
        roundedCornersDp: Int? = 10
    ) {
        loadImage(icon = url?.let(Icon::FromLink)) {
            roundedCornersDp?.let {
                transformations(RoundedCornersTransformation(roundedCornersDp.dpF(context)))
            }

            placeholderRes?.let {
                placeholder(it)
                error(it)
            }
        }
    }

    fun loadImage(
        icon: Icon?,
        extraBuilder: ExtraImageRequestBuilding = { }
    ) {
        if (icon == null) return

        image.makeVisible()
        image.setIcon(icon, imageLoader, extraBuilder)
    }

    fun showProgress() {
        contentGroup.makeGone()
        valueProgress.makeVisible()
    }

    override fun setDividerVisible(visible: Boolean) {
        binder.tableCellValueDivider.setVisible(visible)
    }

    fun setPrimaryValueEndIcon(@DrawableRes icon: Int?, @ColorRes tint: Int? = null) {
        binder.tableCellValuePrimary.setDrawableEnd(icon, widthInDp = 16, paddingInDp = 8, tint = tint)
    }

    fun setPrimaryValueStartIcon(@DrawableRes icon: Int?, @ColorRes tint: Int? = null) {
        binder.tableCellValuePrimary.setDrawableStart(icon, widthInDp = 16, paddingInDp = 8, tint = tint)
    }

    fun setPrimaryValueStyle(style: FieldStyle) {
        when (style) {
            FieldStyle.PRIMARY -> {
                valuePrimary.setTextColorRes(R.color.text_primary)
            }

            FieldStyle.LINK -> {
                valuePrimary.setTextColor(context.getAccentColor())
            }

            FieldStyle.POSITIVE -> {
                valuePrimary.setTextColorRes(R.color.text_positive)
            }

            FieldStyle.SECONDARY -> {
                valuePrimary.setTextColorRes(R.color.text_secondary)
            }
        }
    }

    fun setTitleIconEnd(@DrawableRes icon: Int?, tintRes: Int?) {
        binder.tableCellTitle.setDrawableEnd(icon, widthInDp = 16, paddingInDp = 4, tint = tintRes)
    }

    fun setTitleIconStart(@DrawableRes icon: Int?, tintRes: Int?) {
        binder.tableCellTitle.setDrawableStart(icon, widthInDp = 16, paddingInDp = 4, tint = tintRes)
    }

    fun showValue(primary: CharSequence, secondary: String? = null) {
        postToSelf {
            contentGroup.makeVisible()

            valuePrimary.text = primary
            valueSecondary.setTextOrHide(secondary)

            valueProgress.makeGone()
        }
    }

    fun setTitleEllipsisable(ellipsisable: Boolean) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(this)

        if (ellipsisable) {
            constraintSet.connect(binder.tableCellTitle.id, ConstraintSet.END, binder.barrier.id, ConstraintSet.START, 16.dp(context))
            constraintSet.clear(binder.tableCellValuePrimary.id, ConstraintSet.START)
            constraintSet.constrainedWidth(binder.tableCellTitle.id, true)
            constraintSet.setHorizontalBias(binder.tableCellTitle.id, 0f)
        } else {
            constraintSet.clear(binder.tableCellTitle.id, ConstraintSet.END)
            constraintSet.connect(binder.tableCellValuePrimary.id, ConstraintSet.START, binder.tableCellTitle.id, ConstraintSet.END, 16.dp(context))
            constraintSet.constrainedWidth(binder.tableCellTitle.id, false)
        }
        constraintSet.applyTo(this)
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.TableCellView) { typedArray ->
        val titleText = typedArray.getString(R.styleable.TableCellView_title)
        setTitle(titleText)

        val primaryValueText = typedArray.getString(R.styleable.TableCellView_primaryValue)
        primaryValueText?.let { showValue(it) }

        val dividerVisible = typedArray.getBoolean(R.styleable.TableCellView_dividerVisible, true)
        setDividerVisible(dividerVisible)

        val primaryValueEndIcon = typedArray.getResourceIdOrNull(R.styleable.TableCellView_primaryValueEndIcon)
        primaryValueEndIcon?.let {
            val primaryValueIconTint = typedArray.getResourceIdOrNull(R.styleable.TableCellView_primaryValueIconTint)

            setPrimaryValueEndIcon(primaryValueEndIcon, primaryValueIconTint)
        }

        val primaryValueStartIcon = typedArray.getResourceIdOrNull(R.styleable.TableCellView_primaryValueStartIcon)
        primaryValueStartIcon?.let {
            val primaryValueIconTint = typedArray.getResourceIdOrNull(R.styleable.TableCellView_primaryValueIconTint)

            setPrimaryValueStartIcon(primaryValueStartIcon, primaryValueIconTint)
        }

        val primaryValueStyle = typedArray.getEnum(R.styleable.TableCellView_primaryValueStyle, default = FieldStyle.PRIMARY)
        setPrimaryValueStyle(primaryValueStyle)

        val titleIconEnd = typedArray.getResourceIdOrNull(R.styleable.TableCellView_titleIcon)
        titleIconEnd?.let {
            val titleIconTint = typedArray.getResourceIdOrNull(R.styleable.TableCellView_titleIconTint)

            setTitleIconEnd(titleIconEnd, titleIconTint)
        }

        val titleIconStart = typedArray.getResourceIdOrNull(R.styleable.TableCellView_titleIconStart)
        titleIconStart?.let {
            val titleIconStartTint = typedArray.getResourceIdOrNull(R.styleable.TableCellView_titleIconStartTint)

            setTitleIconStart(titleIconStart, titleIconStartTint)
        }

        val titleTextAppearance = typedArray.getResourceIdOrNull(R.styleable.TableCellView_titleValueTextAppearance)
        titleTextAppearance?.let(title::setTextAppearance)

        val primaryValueTextAppearance = typedArray.getResourceIdOrNull(R.styleable.TableCellView_primaryValueTextAppearance)
        primaryValueTextAppearance?.let(valuePrimary::setTextAppearance)

        val secondaryValueTextAppearance = typedArray.getResourceIdOrNull(R.styleable.TableCellView_secondaryValueTextAppearance)
        secondaryValueTextAppearance?.let(valueSecondary::setTextAppearance)

        val titleEllipsisable = typedArray.getBoolean(R.styleable.TableCellView_titleEllipsisable, false)
        setTitleEllipsisable(titleEllipsisable)
    }
}

fun TableCellView.showValueOrHide(primary: CharSequence?, secondary: String? = null) {
    if (primary != null) {
        showValue(primary, secondary)
    }

    setVisible(primary != null)
}

@Suppress("LiftReturnOrAssignment")
fun TableCellView.setExtraInfoAvailable(available: Boolean) {
    if (available) {
        setPrimaryValueEndIcon(R.drawable.ic_info)
        isEnabled = true
    } else {
        setPrimaryValueEndIcon(null)
        isEnabled = false
    }
}

fun <T> TableCellView.showLoadingState(state: ExtendedLoadingState<T>, showData: (T) -> Unit) {
    when (state) {
        is ExtendedLoadingState.Error -> showValue(context.getString(R.string.common_error_general_title))
        is ExtendedLoadingState.Loaded -> showData(state.data)
        ExtendedLoadingState.Loading -> showProgress()
    }
}

fun TableCellView.showLoadingValue(state: ExtendedLoadingState<String>) {
    showLoadingState(state, ::showValue)
}
