package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.core.view.doOnPreDraw
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.updatePadding

class TableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), WithContextExtensions {

    override val providedContext: Context = context

    init {
        orientation = VERTICAL

        background = getRoundedCornerDrawable(R.color.white_8)
        clipToOutline = true

        doOnPreDraw {
            setupTableChildrenAppearance()
        }
    }

    private fun setupTableChildrenAppearance() {
        val tableChildren = children.filterIsInstance<TableCellView>()
            .toList()

        tableChildren.forEach {
            it.setDividerColor(R.color.white_8)
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
