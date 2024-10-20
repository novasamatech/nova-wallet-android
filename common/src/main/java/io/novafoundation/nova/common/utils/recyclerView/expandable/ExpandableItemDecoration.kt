package io.novafoundation.nova.common.utils.recyclerView.expandable

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.findByStep
import io.novafoundation.nova.common.utils.groupSequentially
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimator
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimationItemState
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableBaseItem
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableParentItem

private class ItemWithViewHolder(val position: Int, val item: ExpandableBaseItem, val viewHolder: RecyclerView.ViewHolder?)

interface GroupAnimationDecoration {
    fun onDraw(
        canvas: Canvas,
        expandableItemState: ExpandableAnimationItemState,
        parent: RecyclerView,
        groupViewHolder: ViewHolder,
        items: List<ViewHolder>
    )
}

abstract class ExpandableItemDecoration(
    private val adapter: ExpandableAdapter,
    private val animator: ExpandableAnimator
) : RecyclerView.ItemDecoration() {

    abstract fun onDrawGroup(
        canvas: Canvas,
        animationState: ExpandableAnimationItemState,
        recyclerView: RecyclerView,
        parent: ViewHolder?,
        children: List<ViewHolder>
    )

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {

    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val items = try {
            getParentAndChildren(parent)
        } catch (e: Exception) {
            emptyMap()
        }

        for ((parentItem, children) in items) {
            val animationState = animator.getStateForPosition(parentItem.position) ?: continue
            val childViewHolders = children.mapNotNull { it.viewHolder }
            onDrawGroup(canvas, animationState, parent, parentItem.viewHolder, childViewHolders)
        }
    }

    private fun getParentAndChildren(parent: RecyclerView): Map<ItemWithViewHolder, List<ItemWithViewHolder>> {
        return parent.children.toList()
            .mapNotNull {
                val position = parent.getChildAdapterPosition(it) - 1
                val viewHolder = parent.getChildViewHolder(it)
                val item = adapter.getItemFor(viewHolder) ?: return@mapNotNull null
                ItemWithViewHolder(position, item, viewHolder)
            }
            .sortedBy { it.position }
            .groupSequentially(
                isKey = { it.item is ExpandableParentItem },
                keyForEmptyValue = { getParentForPosition(it.position) }
            )
    }

    private fun getParentForPosition(position: Int): ItemWithViewHolder {
        val item = adapter.getItems().findByStep(fromPosition = position, -1) { it is ExpandableParentItem }

        return ItemWithViewHolder(position, item as ExpandableParentItem, null)
    }
}
