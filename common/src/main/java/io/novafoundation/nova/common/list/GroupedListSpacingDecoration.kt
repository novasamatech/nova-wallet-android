package io.novafoundation.nova.common.list

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.headers.TextHeaderHolder
import io.novafoundation.nova.common.utils.dp

class GroupedListSpacingDecoration(
    private val groupTopSpacing: Int,
    private val groupBottomSpacing: Int,
    private val firstItemTopSpacing: Int,
    private val middleItemTopSpacing: Int,
    private val itemBottomSpacing: Int,
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val viewHolder = parent.getChildViewHolder(view)

        val isFirst = viewHolder.absoluteAdapterPosition == 0
        val isGroup = viewHolder is TextHeaderHolder

        val context = view.context

        val topDp: Int
        val bottomDp: Int

        when {
            isGroup -> {
                topDp = groupTopSpacing; bottomDp = groupBottomSpacing
            }
            isFirst -> {
                topDp = firstItemTopSpacing; bottomDp = itemBottomSpacing
            }
            else -> {
                topDp = middleItemTopSpacing; bottomDp = itemBottomSpacing
            }
        }

        outRect.set(0, topDp.dp(context), 0, bottomDp.dp(context))
    }
}

fun RecyclerView.setGroupedListSpacings(
    groupTopSpacing: Int = 0,
    groupBottomSpacing: Int = 0,
    firstItemTopSpacing: Int = 0,
    middleItemTopSpacing: Int = 0,
    itemBottomSpacing: Int = 0,
) = addItemDecoration(
    GroupedListSpacingDecoration(
        groupTopSpacing = groupTopSpacing,
        groupBottomSpacing = groupBottomSpacing,
        firstItemTopSpacing = firstItemTopSpacing,
        middleItemTopSpacing = middleItemTopSpacing,
        itemBottomSpacing = itemBottomSpacing
    )
)
