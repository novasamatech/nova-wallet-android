package io.novafoundation.nova.common.list.decoration

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * Implement it in your ViewHolder to set extra space around it.
 */
interface ExtraSpaceViewHolder {

    fun getExtraSpace(topViewHolder: ViewHolder?, bottomViewHolder: ViewHolder?): Rect?
}

/**
 * ItemDecoration that looking for ExtraSpaceViewHolder implementations and set extra space around them.
 */
class ExtraSpaceItemDecoration : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val itemPosition = parent.getChildAdapterPosition(view)
        val viewHolder = parent.findViewHolderForAdapterPosition(itemPosition)
        if (viewHolder is ExtraSpaceViewHolder) {
            val topViewHolder = parent.findViewHolderForAdapterPosition(itemPosition - 1)
            val bottomViewHolder = parent.findViewHolderForAdapterPosition(itemPosition + 1)
            val extraSpace = viewHolder.getExtraSpace(topViewHolder, bottomViewHolder) ?: return
            outRect.set(extraSpace)
        }
    }
}
