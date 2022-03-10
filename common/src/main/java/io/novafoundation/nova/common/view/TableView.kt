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
        updatePadding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)

        background = addRipple(getRoundedCornerDrawable(R.color.white_8))

        doOnPreDraw {
            (children.last() as? TableCellView)?.setDividerVisible(false)
        }
    }
}
