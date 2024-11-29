package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.useAttributes
import kotlinx.android.synthetic.main.view_generic_table_cell.view.genericTableCellTitle
import kotlinx.android.synthetic.main.view_generic_table_cell.view.genericTableCellValueProgress

open class GenericTableCellView<V : View> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0,
) : ConstraintLayout(context, attrs, defStyle, defStyleRes), TableItem {

    protected lateinit var valueView: V

    companion object {

        private val SELF_IDS = listOf(R.id.genericTableCellTitle, R.id.genericTableCellValueProgress)
    }

    init {
        minHeight = 44.dp

        View.inflate(context, R.layout.view_generic_table_cell, this)

        setBackgroundResource(R.drawable.bg_primary_list_item)

        attrs?.let(::applyAttributes)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        findAndPositionValueView()
    }

    @Suppress("UNCHECKED_CAST")
    private fun findAndPositionValueView() {
        children.forEach {
            if (it.id !in SELF_IDS) {
                valueView = it as V
                valueView.layoutParams = createValueViewLayoutParams()
                requestLayout()
            }
        }
    }

    override fun addView(child: View) {
        if (child.id in SELF_IDS) {
            super.addView(child)
        } else {
            addValueView(child)
        }
    }

    fun showProgress(showProgress: Boolean) {
        genericTableCellValueProgress.setVisible(showProgress)
        valueView.setVisible(!showProgress)
    }

    fun setTitle(title: String?) {
        genericTableCellTitle.text = title
    }

    fun setTitle(@StringRes titleRes: Int) {
        genericTableCellTitle.setText(titleRes)
    }

    fun setTitleIconEnd(@DrawableRes icon: Int?) {
        genericTableCellTitle.setDrawableEnd(icon, widthInDp = 16, paddingInDp = 4)
    }

    @JvmName("setValueContentView")
    protected fun setValueView(view: V) {
        addValueView(view)
    }

    @Suppress("UNCHECKED_CAST")
    private fun addValueView(child: View) {
        valueView = child as V
        super.addView(child, createValueViewLayoutParams())
    }

    private fun createValueViewLayoutParams() = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
        endToEnd = LayoutParams.PARENT_ID
        startToEnd = R.id.genericTableCellTitle
        marginStart = 16.dp
        topToTop = LayoutParams.PARENT_ID
        bottomToBottom = LayoutParams.PARENT_ID
        horizontalBias = 1.0f
        constrainedWidth = true
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.GenericTableCellView) { typedArray ->
        val titleText = typedArray.getString(R.styleable.GenericTableCellView_title)
        setTitle(titleText)

        val titleIconEnd = typedArray.getResourceIdOrNull(R.styleable.GenericTableCellView_titleIcon)
        titleIconEnd?.let(::setTitleIconEnd)
    }

    override fun disableOwnDividers() {}

    override fun shouldDrawDivider(): Boolean {
        return true
    }
}
