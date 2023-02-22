package io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.list

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.feature_governance_impl.R

class VoterItemDecoration(
    private val context: Context,
    private val adapter: GroupedListAdapter<*, *>
) : RecyclerView.ItemDecoration() {

    private val treePath = Path()
    private val delegatorsStartOffset: Int = 44.dp(context)
    private val treeStrokeStartOffset: Float = 28.dpF(context)
    private val treePointerLength: Float = 11.dpF(context)

    private val expandableBlockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.block_background)
    }

    private val treePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.block_background)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.MITER
        strokeWidth = 2.dpF(context)
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val viewHolder = parent.getChildViewHolder(view)
        if (viewHolder is VoterDelegatorHolder) {
            view.updatePadding(start = delegatorsStartOffset)
        }
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val linearLayoutManager = parent.layoutManager as LinearLayoutManager

        var filling = false
        val expandableRanges = mutableListOf<ExpandableItemRange>()

        for (childIndex in 0 until linearLayoutManager.childCount) {
            val (view, viewHolder) = linearLayoutManager.getViewAndViewHolder(parent, childIndex)

            if (view == null) continue

            if (viewHolder is VoterDelegatorHolder) {
                if (!filling) {
                    expandableRanges.add(ExpandableItemRange(linearLayoutManager, adapter))
                    filling = true
                }
                expandableRanges.last()
                    .add(childIndex, view)
            } else if (filling && viewHolder is ExpandableVoterHolder) {
                filling = false
            }
        }

        expandableRanges.forEach {
            val left = 0f
            val top = it.getTopEdge()
            val right = parent.measuredWidth.toFloat()
            val bottom = it.getBottomEdge()

            canvas.save()
            canvas.clipRect(left, top, right, bottom)

            canvas.drawRect(left, top, right, bottom, expandableBlockPaint)

            treePath.reset()
            it.buildTreePath(treePath, treeStrokeStartOffset, treePointerLength)
            canvas.drawPath(treePath, treePaint)

            canvas.restore()
        }
    }

    private fun LinearLayoutManager.getViewAndViewHolder(parent: RecyclerView, index: Int): Pair<View?, RecyclerView.ViewHolder?> {
        if (index >= childCount) return null to null

        val view = this.getChildAt(index) ?: return null to null
        val viewHolder = parent.getChildViewHolder(view)

        return view to viewHolder
    }

    class IndexAndView(val index: Int, val view: View)

    class ExpandableItemRange(
        private val linearLayoutManager: LinearLayoutManager,
        private val adapter: GroupedListAdapter<*, *>
    ) {

        private val expandedItems: MutableList<IndexAndView> = mutableListOf()

        fun add(index: Int, view: View) {
            expandedItems.add(IndexAndView(index, view))
        }

        fun buildTreePath(treePath: Path, treeStrokeStartOffset: Float, treePointerLength: Float) {
            expandedItems.forEach {
                val y = it.view.y + it.view.pivotY
                treePath.moveTo(treeStrokeStartOffset + treePointerLength, y)
                treePath.lineTo(treeStrokeStartOffset, y)
            }

            val lastItem = expandedItems.last().view
            val lastItemAdapterPosition = linearLayoutManager.getPosition(lastItem)
            val nextItemAdapterPosition = lastItemAdapterPosition + 1
            if (nextItemAdapterPosition < adapter.itemCount && adapter.getItemViewType(nextItemAdapterPosition) == GroupedListAdapter.TYPE_CHILD) {
                treePath.moveTo(treeStrokeStartOffset, lastItem.bottom.toFloat() + lastItem.translationY)
            }

            val firstItem = expandedItems.first().view
            treePath.lineTo(treeStrokeStartOffset, firstItem.top.toFloat() + firstItem.translationY)
        }

        fun getTopEdge(): Float {
            val firstItem = expandedItems.first()
            val anchorView = linearLayoutManager.getChildAt(firstItem.index - 1)
            return if (anchorView == null) {
                firstItem.view.top.toFloat() + firstItem.view.translationY
            } else {
                anchorView.bottom.toFloat() + anchorView.translationY
            }
        }

        fun getBottomEdge(): Float {
            val lastItem = expandedItems.last()
            val anchorView = linearLayoutManager.getChildAt(lastItem.index + 1)
            return if (anchorView == null) {
                lastItem.view.bottom.toFloat() + lastItem.view.translationY
            } else {
                anchorView.top.toFloat() + anchorView.translationY
            }
        }
    }
}
