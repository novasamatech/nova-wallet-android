package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateMarginsRelative
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.common.utils.useAttributes

open class TableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), WithContextExtensions {

    override val providedContext: Context = context

    val titleView: TextView

    init {
        orientation = VERTICAL

        background = getRoundedCornerDrawable(R.color.white_8)
        clipToOutline = true

        titleView = addTitleView()

        attrs?.let(::applyAttributes)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        setupTableChildrenAppearance()
    }

    fun setTitle(title: String?) {
        titleView.setTextOrHide(title)
    }

    private fun addTitleView(): TextView = TextView(context).also { title ->
        title.setTextAppearance(R.style.TextAppearance_NovaFoundation_Regular_SubHeadline)
        title.setTextColorRes(R.color.white)
        title.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also { params ->
            params.updateMarginsRelative(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
        }

        addView(title, 0)
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.TableView) {
        val title = it.getString(R.styleable.TableView_title)
        setTitle(title)
    }

    private fun setupTableChildrenAppearance() {
        val tableChildren = children.filterIsInstance<TableCellView>()
            .filter { it.isVisible }
            .toList()

        if (tableChildren.isEmpty()) {
            makeGone()
            return
        } else {
            makeVisible()
        }

        tableChildren.forEach {
            it.setDividerColor(R.color.white_8)
            it.setDividerVisible(true)
            it.updatePadding(start = 16.dp, end = 16.dp)
        }

        tableChildren.first().apply {
            updatePadding(top = 4.dp)
        }
        tableChildren.last().apply {
            updatePadding(bottom = 4.dp)
            setDividerVisible(false)
        }
    }
}
