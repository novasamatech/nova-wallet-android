package io.novafoundation.nova.feature_swap_impl.presentation.route.list

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.feature_swap_impl.R


class TimelineItemDecoration(
    context: Context,
    private val shouldDecorate: (RecyclerView.ViewHolder) -> Boolean
) : RecyclerView.ItemDecoration(),
    WithContextExtensions by WithContextExtensions(context) {


    private val linePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.timeline_line_color)
        strokeWidth = 1.dpF
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(4.dpF, 4.dpF), 0f)
    }

    private val circlePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.timeline_circle_color)
        isAntiAlias = true
    }

    private val textPaint = createTextPaint()

    private val circleRadius = 10.dp

    private val itemTopMargin = 12.dp

    private val lineHorizontalMargin = 18.dp

    private val lineToCircleMargin = 4.dp

    private val itemStartMargin = (lineHorizontalMargin + circleRadius) * 2
    private val itemEndMargin = 16.dp

    private val circleTopMargin = 10.dp

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val viewHolder = parent.getChildViewHolder(child)
            if (!shouldDecorate(viewHolder)) continue

            val position = viewHolder.bindingAdapterPosition
            val centerX = lineHorizontalMargin + circleRadius.toFloat()

            val circleCenterY = (child.top + circleTopMargin + circleRadius).toFloat()

            if (position < viewHolder.bindingAdapter!!.itemCount - 1) {
                val childSpaceEndY = child.bottom + itemTopMargin
                val nextCircleTopY = childSpaceEndY + circleTopMargin

                canvas.drawLine(
                    centerX,
                    circleCenterY + circleRadius + lineToCircleMargin,
                    centerX,
                    nextCircleTopY - lineToCircleMargin.toFloat(),
                    linePaint
                )
            }

            canvas.drawCircle(centerX, circleCenterY, circleRadius.toFloat(), circlePaint)

            val text = (position + 1).toString()
            val textY = circleCenterY - (textPaint.descent() + textPaint.ascent()) / 2

            canvas.drawText(text, centerX, textY, textPaint)
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val viewHolder = parent.getChildViewHolder(view)
        if (shouldDecorate(viewHolder)) {
            outRect.left = itemStartMargin
            outRect.top = itemTopMargin
            outRect.right = itemEndMargin
        }
    }

    private fun createTextPaint(): Paint {
        val textView: TextView = AppCompatTextView(providedContext)
        TextViewCompat.setTextAppearance(textView, R.style.TextAppearance_NovaFoundation_SemiBold_Caps1)
        return textView.paint.apply {
            color = ContextCompat.getColor(providedContext, R.color.text_secondary)
            textAlign = Paint.Align.CENTER
        }
    }
}
