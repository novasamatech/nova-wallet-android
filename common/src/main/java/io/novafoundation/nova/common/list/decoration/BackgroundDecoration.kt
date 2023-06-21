package io.novafoundation.nova.common.list.decoration

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import kotlin.math.roundToInt

open class BackgroundDecoration(
    context: Context,
    private val background: Drawable = context.getRoundedCornerDrawable(fillColorRes = R.color.block_background),
    outerHorizontalMarginDp: Int,
    innerVerticalPaddingDp: Int,
) : RecyclerView.ItemDecoration() {

    private val innerVerticalPadding = innerVerticalPaddingDp.dp(context)
    private val outerHorizontalMargin = outerHorizontalMarginDp.dp(context)

    open fun shouldApplyDecoration(holder: RecyclerView.ViewHolder): Boolean {
        return true
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (!parent.shouldApplyDecoration(view)) return

        outRect.set(outerHorizontalMargin, 0, outerHorizontalMargin, 0)
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val children = filterHeader(parent)
        val topChild: View = children.mostTop() ?: return
        val bottomChild: View = children.mostBottom() ?: return

        background.setBounds(
            topChild.left,
            topChild.top + topChild.translationY.roundToInt() - innerVerticalPadding,
            topChild.right,
            bottomChild.bottom + bottomChild.translationY.roundToInt() + innerVerticalPadding
        )

        background.draw(canvas)
    }

    private fun filterHeader(parent: RecyclerView): List<View> {
        return parent.children
            .filter { parent.shouldApplyDecoration(it) }
            .toList()
    }

    private fun RecyclerView.shouldApplyDecoration(view: View): Boolean {
        val viewHolder = getChildViewHolder(view)
        return shouldApplyDecoration(viewHolder)
    }

    private fun List<View>.mostTop(): View? {
        return minByOrNull { it.top + it.translationY }
    }

    private fun List<View>.mostBottom(): View? {
        return maxByOrNull { it.bottom + it.translationY }
    }
}
