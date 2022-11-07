package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_dapp_impl.R
import kotlin.math.roundToInt

class DAppItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    val drawable = context.getRoundedCornerDrawable(fillColorRes = R.color.black_48)
    val drawableVerticalPadding = 12.dp(context)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (!parent.shouldApplyDecoration(view)) return

        outRect.set(16.dp(view.context), 0, 16.dp(view.context), 0)
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val children = filterHeader(parent)
        val topChild: View = children.mostTop() ?: return
        val bottomChild: View = children.mostBottom() ?: return

        drawable.setBounds(
            topChild.left,
            topChild.top + topChild.translationY.roundToInt() - drawableVerticalPadding,
            topChild.right,
            bottomChild.bottom + bottomChild.translationY.roundToInt() + drawableVerticalPadding
        )

        drawable.draw(canvas)
    }

    private fun filterHeader(parent: RecyclerView): List<View> {
        return parent.children
            .filter { parent.shouldApplyDecoration(it) }
            .toList()
    }

    private fun RecyclerView.shouldApplyDecoration(view: View): Boolean {
        val viewHolder = getChildViewHolder(view)
        return viewHolder !is HeaderHolder
    }

    private fun List<View>.mostTop(): View? {
        return minByOrNull { it.top + it.translationY }
    }

    private fun List<View>.mostBottom(): View? {
        return maxByOrNull { it.bottom + it.translationY }
    }
}
