package io.novafoundation.nova.feature_assets.presentation.balance.common

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import androidx.core.graphics.toRect
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableAdapter
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableItemDecoration
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimationItemState
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimator
import io.novafoundation.nova.common.utils.recyclerView.expandable.expandingFraction
import io.novafoundation.nova.common.utils.recyclerView.expandable.flippedFraction
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableParentItem
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.balance.common.holders.TokenAssetGroupViewHolder
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import kotlin.math.roundToInt

class AssetTokensDecoration(
    private val context: Context,
    adapter: ExpandableAdapter,
    animator: ExpandableAnimator
) : ExpandableItemDecoration(
    adapter,
    animator
) {
    private val argbEvaluator = ArgbEvaluator()

    private val childrenBlockCollapsedHorizontalMargin = 16.dp(context)
    private val childrenBlockCollapsedHeight = 4.dp(context)

    private val blockRadiusCollapsed = 4.dpF(context)
    private val blockRadiusExpanded = 12.dpF(context)
    private val blockRadiusDelta = blockRadiusExpanded - blockRadiusCollapsed

    private val blockColor = context.getColor(R.color.block_background)
    private val hidedBlockColor = context.getColor(R.color.hided_networks_block_background)
    private val transparentColor = Color.TRANSPARENT
    private val dividerColor = context.getColor(R.color.divider)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private var drawingPath = Path()

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val viewHolder = parent.getChildViewHolder(view)

        if (viewHolder is TokenAssetGroupViewHolder) {
            outRect.set(0, 12.dp(context), 0, 0)
            return
        }
    }

    override fun onDrawGroup(
        canvas: Canvas,
        animationState: ExpandableAnimationItemState,
        recyclerView: RecyclerView,
        parentItem: ExpandableParentItem,
        parent: RecyclerView.ViewHolder?,
        children: List<RecyclerView.ViewHolder>
    ) {
        val expandingFraction = animationState.expandingFraction()

        val parentBounds = parentBounds(parent)
        if (parentBounds != null) {
            drawParentBlock(parentBounds, canvas, expandingFraction)
        }

        // Don't draw children background if it's a single item
        if (parentItem is TokenGroupUi && parentItem.groupWithOneItem) return

        val childrenBlockBounds = getChildrenBlockBounds(animationState, recyclerView, parent, children)
        drawChildrenBlock(expandingFraction, childrenBlockBounds, canvas)
        clipChildren(children, childrenBlockBounds)
    }

    private fun clipChildren(children: List<RecyclerView.ViewHolder>, childrenBlockBounds: RectF) {
        val childrenBlock = childrenBlockBounds.toRect()
        children.forEach {
            val childrenBottomClipInset = (it.itemView.bottom + it.itemView.translationY.roundToInt()) - childrenBlock.bottom
            val childrenTopClipInset = childrenBlock.top - (it.itemView.top + it.itemView.translationY.roundToInt())
            if (childrenBottomClipInset > 0) {
                it.itemView.clipBounds = Rect(
                    0,
                    childrenTopClipInset.coerceAtLeast(0),
                    it.itemView.width,
                    it.itemView.height - childrenBottomClipInset
                )
            } else {
                it.itemView.clipBounds = null
            }
        }
    }

    private fun drawChildrenBlock(expandingFraction: Float, childrenBlockBounds: RectF, canvas: Canvas) {
        val animatedBlockRadius = blockRadiusDelta * expandingFraction
        childrenBlockBounds.toPath(drawingPath, topRadius = 0f, bottomRadius = blockRadiusCollapsed + animatedBlockRadius * expandingFraction)
        paint.color = argbEvaluator.evaluate(expandingFraction, hidedBlockColor, blockColor) as Int
        canvas.drawPath(drawingPath, paint)
    }

    private fun drawParentBlock(
        parentBounds: RectF,
        canvas: Canvas,
        expandingFraction: Float
    ) {
        val path = Path()
        val bottomRadius = blockRadiusExpanded * expandingFraction.flippedFraction()
        parentBounds.toPath(path, topRadius = blockRadiusExpanded, bottomRadius = bottomRadius)
        paint.color = blockColor
        canvas.drawPath(path, paint)

        drawParentDivider(expandingFraction, bottomRadius, canvas, parentBounds)
    }

    private fun drawParentDivider(
        expandingFraction: Float,
        dividerHorizontalMargin: Float,
        canvas: Canvas,
        parentBounds: RectF
    ) {
        linePaint.color = argbEvaluator.evaluate(expandingFraction, transparentColor, dividerColor) as Int
        canvas.drawLine(
            parentBounds.left + dividerHorizontalMargin,
            parentBounds.bottom,
            parentBounds.right - dividerHorizontalMargin,
            parentBounds.bottom,
            linePaint
        )
    }

    private fun parentBounds(parent: RecyclerView.ViewHolder?): RectF? {
        if (parent == null) return null

        return parent.itemView.let {
            RectF(
                it.left.toFloat(),
                it.top.toFloat() + it.translationY,
                it.right.toFloat(),
                it.bottom.toFloat() + it.translationY
            )
        }
    }

    private fun getChildrenBlockBounds(
        animationState: ExpandableAnimationItemState,
        recyclerView: RecyclerView,
        parent: RecyclerView.ViewHolder?,
        children: List<RecyclerView.ViewHolder>
    ): RectF {
        val lastChild = children.maxByOrNull { it.itemView.bottom }

        val parentTranslationY = parent?.itemView?.translationY ?: 0f
        val childTranslationY = lastChild?.itemView?.translationY ?: 0f

        val top = (parent?.itemView?.bottom ?: recyclerView.top) + parentTranslationY
        val bottom = (lastChild?.itemView?.bottom?.toFloat() ?: top).coerceAtLeast(top)
        val left = parent?.itemView?.left ?: lastChild?.itemView?.left ?: recyclerView.left
        val right = parent?.itemView?.right ?: lastChild?.itemView?.right ?: recyclerView.right

        val expandingFraction = animationState.expandingFraction()
        val flippedExpandingFraction = expandingFraction.flippedFraction()
        val heightDelta = (bottom - top)
        return RectF(
            left + childrenBlockCollapsedHorizontalMargin * flippedExpandingFraction,
            top,
            right - childrenBlockCollapsedHorizontalMargin * flippedExpandingFraction,
            top + childrenBlockCollapsedHeight + heightDelta * expandingFraction + childTranslationY
        )
    }

    private fun RectF.toPath(path: Path, topRadius: Float, bottomRadius: Float) {
        path.reset()
        path.addRoundRect(
            this,
            floatArrayOf(topRadius, topRadius, topRadius, topRadius, bottomRadius, bottomRadius, bottomRadius, bottomRadius),
            Path.Direction.CW
        )
    }
}
