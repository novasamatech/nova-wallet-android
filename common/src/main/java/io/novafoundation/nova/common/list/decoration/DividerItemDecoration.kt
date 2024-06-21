package io.novafoundation.nova.common.list.decoration

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.dpF

open class DividerItemDecoration(
    context: Context,
    private val dividerColorRes: Int = R.color.divider,
    dividerWidthDp: Int = 1,
    dividerMarginDp: Int = 0
) : RecyclerView.ItemDecoration() {

    private val dividerMargin = dividerMarginDp.dp(context)

    private val paint = Paint().apply {
        color = context.getColor(dividerColorRes)
        style = Paint.Style.FILL
        strokeWidth = dividerWidthDp.dpF(context)
    }

    open fun shouldApplyDecorationBetween(top: RecyclerView.ViewHolder, bottom: RecyclerView.ViewHolder): Boolean {
        return true
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        filterChildren(parent)
            .forEach {
                canvas.drawLine(
                    dividerMargin.toFloat(),
                    it.bottom.toFloat(),
                    parent.width.toFloat() - dividerMargin,
                    it.bottom.toFloat(),
                    paint
                )
            }
    }

    /**
     * Returns children that should have divider under them.
     */
    private fun filterChildren(parent: RecyclerView): List<View> {
        return parent.children
            .zipWithNext()
            .filter { (top, bottom) -> parent.shouldApplyDecoration(top, bottom) }
            .map { it.first }
            .toList()
    }

    private fun RecyclerView.shouldApplyDecoration(top: View, bottom: View): Boolean {
        val topViewHolder = getChildViewHolder(top)
        val bottomViewHolder = getChildViewHolder(bottom)
        return shouldApplyDecorationBetween(topViewHolder, bottomViewHolder)
    }
}
