package io.novafoundation.nova.common.utils.recyclerView.expandable

import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import android.view.View
import androidx.core.view.children
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.indexOfFirstOrNull
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimator
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimationItemState
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableBaseItem
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableChildItem
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableParentItem
import kotlin.system.measureTimeMillis

private data class ItemWithViewHolder(val position: Int, val item: ExpandableBaseItem, val viewHolder: ViewHolder?)

abstract class ExpandableItemDecoration(
    private val adapter: ExpandableAdapter,
    private val animator: ExpandableAnimator
) : RecyclerView.ItemDecoration() {

    abstract fun onDrawGroup(
        canvas: Canvas,
        animationState: ExpandableAnimationItemState,
        recyclerView: RecyclerView,
        parentItem: ExpandableParentItem,
        parent: ViewHolder?,
        children: List<ViewHolder>
    )

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
    }

    override fun onDraw(canvas: Canvas, recyclerView: RecyclerView, state: RecyclerView.State) {
        val items = getParentAndChildren(recyclerView)
        for ((parentItem, children) in items) {
            val animationState = animator.getStateForPosition(parentItem.position) ?: continue
            val childViewHolders = children.mapNotNull { it.viewHolder }
            onDrawGroup(canvas, animationState, recyclerView, parentItem.item as ExpandableParentItem, parentItem.viewHolder, childViewHolders)
        }
    }

    private fun getParentAndChildren(recyclerView: RecyclerView): Map<ItemWithViewHolder, List<ItemWithViewHolder>> {
        // Searching all view holders in recycler view and match them with adapter items
        val items = recyclerView.children.toList()
            .mapNotNull {
                val viewHolder = recyclerView.getChildViewHolder(it)
                val expandableViewHolder = viewHolder as? ExpandableBaseViewHolder<*> ?: return@mapNotNull null
                val item = expandableViewHolder.expandableItem ?: return@mapNotNull null
                ItemWithViewHolder(viewHolder.bindingAdapterPosition, item, viewHolder)
            }

        // Grouping view holders by parents
        val parentsWithChildren = mutableMapOf<ItemWithViewHolder, MutableList<ItemWithViewHolder>>()

        val parents = items.filter { it.item is ExpandableParentItem }.associateBy { it.item.getId() }
        val children = items.filter { it.item is ExpandableChildItem }
        parents.values.forEach { parentsWithChildren[it] = mutableListOf() }

        children.forEach { child ->
            val item = child.item as ExpandableChildItem
            val parent = parents[item.groupId] ?: getParentForItem(recyclerView, item) ?: return@forEach
            val parentChildren = parentsWithChildren[parent] ?: mutableListOf()
            parentChildren.add(child)
            parentsWithChildren[parent] = parentChildren
        }

        return parentsWithChildren
    }

    private fun getParentForItem(recyclerView: RecyclerView, item: ExpandableChildItem): ItemWithViewHolder? {
        val positionInAdapter = adapter.getItems().indexOfFirstOrNull { it.getId() == item.groupId } ?: return null
        val parentItem = adapter.getItems()[positionInAdapter]
        val globalAdapterPosition = positionInAdapter.convertToGlobalAdapterPosition(recyclerView, adapter as Adapter<*>)
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(globalAdapterPosition)
        return ItemWithViewHolder(positionInAdapter, parentItem as ExpandableParentItem, viewHolder)
    }

    // Useful to find global position if ConcatAdapter is used
    private fun Int.convertToGlobalAdapterPosition(recyclerView: RecyclerView, localAdapter: Adapter<*>): Int {
        val globalAdapter = recyclerView.adapter
        return if (globalAdapter is ConcatAdapter) {
            val localAdapterIndex = globalAdapter.adapters.indexOf(localAdapter)
            if (localAdapterIndex > 0) {
                val adaptersBeforeTarget = globalAdapter.adapters.subList(0, localAdapterIndex - 1)
                val offset = adaptersBeforeTarget.sumOf { it.itemCount }
                this + offset
            } else {
                this
            }
        } else {
            this
        }
    }
}
