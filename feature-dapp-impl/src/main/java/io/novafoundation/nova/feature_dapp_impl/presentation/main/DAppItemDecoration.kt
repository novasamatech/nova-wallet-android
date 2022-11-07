package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import android.view.View
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.NestedListViewHolder
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_dapp_impl.R
import kotlin.math.roundToInt

class DAppItemDecoration(context: Context, val dappRecyclerView: RecyclerView) : RecyclerView.ItemDecoration() {

    val drawable = context.getRoundedCornerDrawable(fillColorRes = R.color.black_48)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val viewHolder = parent.getChildViewHolder(view)
        if (viewHolder is HeaderHolder) return

        outRect.set(16.dp(view.context), 0, 16.dp(view.context), 0)
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val children = filterHeader(parent)
        val topChild: View = children.mostTop() ?: return
        val bottomChild: View = children.mostBottom() ?: return

        val hasCategories = parent.children.toList().any { parent.getChildViewHolder(it) is NestedListViewHolder<*, *> }
        Log.d("VIEW_HOLDER", hasCategories.toString() + " " + parent.getChildViewHolder(topChild).javaClass.toString())

        drawable.setBounds(
            topChild.left,
            topChild.top - 12.dp(parent.context) + topChild.translationY.roundToInt(),
            topChild.right,
            bottomChild.bottom + 12.dp(parent.context) + bottomChild.translationY.roundToInt()
        )

        drawable.draw(canvas)
    }

    private fun filterHeader(parent: RecyclerView): List<View> {
        return parent.children.filter {
            val viewHolder = parent.getChildViewHolder(it)
            viewHolder !is HeaderHolder
        }.toList()
    }

    private fun List<View>.mostTop(): View? {
        return minByOrNull { it.top + it.translationY }
    }

    private fun List<View>.mostBottom(): View? {
        return maxByOrNull { it.bottom + it.translationY }
    }
}
