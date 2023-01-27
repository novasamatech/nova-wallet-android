package io.novafoundation.nova.feature_versions_impl.presentation.update

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.feature_versions_impl.R
import io.novafoundation.nova.feature_versions_impl.presentation.update.adapters.UpdateNotificationHolder
import kotlin.math.roundToInt

class UpdateNotificationsItemDecoration(
    val context: Context
) : RecyclerView.ItemDecoration() {

    private val dividerColor: Int = context.getColor(R.color.divider)
    private val dividerSize: Float = 1.dpF(context)
    private val paddingHorizontal: Float = 16.dpF(context)
    private val dividerOffset = dividerSize / 2
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dividerSize
        color = dividerColor
        style = Paint.Style.STROKE
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val index = parent.layoutManager!!.getPosition(view)
        if (!parent.shouldApplyDecoration(parent, index, view)) return

        outRect.set(0, 0, 0, dividerSize.roundToInt())
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val children = filterItems(parent)
        children.forEach { view ->
            val dividerY = view.bottom + dividerOffset
            canvas.drawLine(paddingHorizontal, dividerY, view.right - paddingHorizontal, dividerY, paint)
        }
    }

    private fun filterItems(parent: RecyclerView): List<View> {
        return parent.children
            .toList()
            .filterIndexed { index, view -> parent.shouldApplyDecoration(parent, index, view) }
    }

    private fun RecyclerView.shouldApplyDecoration(parent: RecyclerView, index: Int, view: View): Boolean {
        val thisViewHolder = getChildViewHolder(view)
        val nextViewHolder = getChildAt(index + 1)?.let { getChildViewHolder(it) }
        return thisViewHolder is UpdateNotificationHolder && nextViewHolder is UpdateNotificationHolder
    }
}
